package com.example.mahjong.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
// JWT 工具类：负责创建 token 和从 token 里解析 userId。
public class JwtUtil {

    // 用于签名和验签的密钥。
    private final SecretKey secretKey;
    // token 有效期。
    private final Duration ttl;

    public JwtUtil(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.ttl-hours:168}") long ttlHours
    ) {
        // jjwt 要求 HMAC 密钥有足够长度，配置里至少要 32 字节。
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(ttlHours);
    }

    // 根据用户 ID 创建登录 token。
    public String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
            // subject 用来保存当前登录用户 ID。
            .subject(String.valueOf(userId))
            .issuedAt(now)
            .expiration(new Date(now.getTime() + ttl.toMillis()))
            .signWith(secretKey)
            .compact();
    }

    // 校验 token 签名和过期时间，并取出用户 ID。
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
