package com.nxg.mall.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 1. 修改返回类型为 SecretKey
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                // 2. 这里现在可以正常识别了
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                // 3. 传入 SecretKey 后，verifyWith 就能匹配到正确的重载
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 因为生成时用的是 String.valueOf(userId)，所以这里拿到的 subject 是 String
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            // 如果 Token 过期、签名不对或格式错误，parseToken 会抛出异常
            return null;
        }
    }
}