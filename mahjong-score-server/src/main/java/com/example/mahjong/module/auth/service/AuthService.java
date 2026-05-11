package com.example.mahjong.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.common.api.ErrorCode;
import com.example.mahjong.common.exception.BizException;
import com.example.mahjong.common.security.JwtUtil;
import com.example.mahjong.module.auth.dto.AuthDtos;
import com.example.mahjong.module.user.entity.SysUser;
import com.example.mahjong.module.user.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
// 认证业务服务：处理微信登录、用户创建、手机号绑定。
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${app.wechat.app-id:}")
    private String appId;

    @Value("${app.wechat.app-secret:}")
    private String appSecret;

    // 微信登录主流程：code -> openid -> 查/建用户 -> 签发 token。
    @Transactional
    public AuthDtos.WechatLoginResponse wechatLogin(AuthDtos.WechatLoginRequest request) {
        String openid = resolveOpenid(request.getCode());
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenid, openid));
        if (user == null) {
            // 第一次登录的微信用户，创建本系统用户记录。
            user = new SysUser();
            user.setOpenid(openid);
            user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "微信用户");
            user.setAvatarUrl(request.getAvatarUrl());
            user.setTotalScore(0);
            user.setTotalRounds(0);
            sysUserMapper.insert(user);
        }

        // 登录成功后返回 token 和基本资料，前端会保存 token。
        AuthDtos.WechatLoginResponse response = new AuthDtos.WechatLoginResponse();
        response.setToken(jwtUtil.createToken(user.getId()));
        response.setUserId(user.getId());
        response.setHasBindPhone(StringUtils.hasText(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
    }

    // 绑定手机号主流程：校验用户 -> 换手机号 -> 防重复绑定 -> 保存手机号。
    @Transactional
    public AuthDtos.BindPhoneResponse bindPhone(Long userId, AuthDtos.BindPhoneRequest request) {
        SysUser user = requireUser(userId);
        if (StringUtils.hasText(user.getPhone())) {
            // 第一版产品约定不支持换绑，避免账号归属复杂化。
            throw new BizException(ErrorCode.FORBIDDEN, "手机号绑定后暂不支持换绑");
        }
        String phone = resolvePhone(request.getPhoneCode());
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
        if (exists != null) {
            // 手机号唯一，不能绑定到两个账号上。
            throw new BizException(ErrorCode.FORBIDDEN, "该手机号已绑定其他账号");
        }
        user.setPhone(phone);
        sysUserMapper.updateById(user);

        AuthDtos.BindPhoneResponse response = new AuthDtos.BindPhoneResponse();
        response.setPhone(maskPhone(phone));
        return response;
    }

    // 根据用户 ID 查询用户，不存在时统一抛未登录异常。
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    // 解析微信 openid。没有配置微信 appId/appSecret 时走开发环境模拟逻辑。
    private String resolveOpenid(String code) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            // 开发环境用 code 的 MD5 伪造一个稳定 openid，方便本地调试。
            return "dev_" + DigestUtils.md5DigestAsHex(code.getBytes(StandardCharsets.UTF_8));
        }
        // 生产环境调用微信 jscode2session 接口。
        String body = restClient.get()
            .uri("https://api.weixin.qq.com/sns/jscode2session?appid={appId}&secret={secret}&js_code={code}&grant_type=authorization_code",
                appId, appSecret, code)
            .retrieve()
            .body(String.class);
        try {
            // 微信返回 JSON，成功时里面会有 openid。
            JsonNode node = objectMapper.readTree(body);
            String openid = node.path("openid").asText();
            if (!StringUtils.hasText(openid)) {
                throw new BizException("微信登录失败");
            }
            return openid;
        } catch (Exception ex) {
            throw new BizException("微信登录失败");
        }
    }

    // 解析手机号。生产环境走微信接口，开发环境允许直接传 11 位手机号。
    private String resolvePhone(String phoneCode) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            String digits = phoneCode.replaceAll("\\D", "");
            if (digits.matches("1\\d{10}")) {
                // 开发时输入真实格式手机号就直接使用。
                return digits;
            }
            // 如果输入不是手机号，也生成一个稳定的模拟手机号。
            String suffix = String.format("%04d", Math.abs(phoneCode.hashCode()) % 10000);
            return "1380000" + suffix;
        }
        String accessToken = getWechatAccessToken();
        // 生产环境用微信手机号能力接口，通过临时 code 换 phoneNumber。
        String body = restClient.post()
            .uri("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={accessToken}", accessToken)
            .body("{\"code\":\"" + phoneCode + "\"}")
            .retrieve()
            .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            String phone = node.path("phone_info").path("phoneNumber").asText();
            if (!StringUtils.hasText(phone)) {
                throw new BizException("手机号授权失败");
            }
            return phone;
        } catch (Exception ex) {
            throw new BizException("手机号授权失败");
        }
    }

    // 获取调用微信服务端接口需要的 access_token。
    private String getWechatAccessToken() {
        String body = restClient.get()
            .uri("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appId}&secret={secret}", appId, appSecret)
            .retrieve()
            .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            String token = node.path("access_token").asText();
            if (!StringUtils.hasText(token)) {
                throw new BizException("微信 access_token 获取失败");
            }
            return token;
        } catch (Exception ex) {
            throw new BizException("微信 access_token 获取失败");
        }
    }

    // 手机号脱敏，只给前端展示前三后四。
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
