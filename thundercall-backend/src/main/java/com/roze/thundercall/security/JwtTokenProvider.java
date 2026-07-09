package com.roze.thundercall.security;

import com.roze.thundercall.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;
    private Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(jwtExpiration);
        return Jwts.builder().subject(Long.toString(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(getSignInKey()).compact();
    }

    public Boolean validateToken(String authToken) {
        try {
            Jwts
                    .parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parse(authToken);
            return true;
        } catch (Exception e) {
            log.error("Jwt validation error", e);
        }
        return false;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(getSignInKey())
                .build().parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
