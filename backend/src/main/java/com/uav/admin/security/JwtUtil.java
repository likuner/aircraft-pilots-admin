package com.uav.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 工具类：HS256 签发 / 解析
 */
@Component
public class JwtUtil {

    @Value("${uav.jwt.secret}")
    private String secret;

    @Value("${uav.jwt.expire}")
    private long expire;

    @Value("${uav.jwt.refresh-expire}")
    private long refreshExpire;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 access token
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, expire);
    }

    /**
     * 生成 refresh token
     */
    public String generateRefreshToken(Long userId, String username) {
        return buildToken(userId, username, null, refreshExpire);
    }

    private String buildToken(Long userId, String username, List<String> roles, long ttl) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 token，失败抛异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        return claims.get("roles", List.class);
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }
}
