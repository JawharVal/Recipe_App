package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JWTGenerator {
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    @Autowired
    private UserRepository userRepository;

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        String userRole = user.getRole();
        Long userId = user.getId();

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + SecurityConstants.JWT_EXPIRATION);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        @SuppressWarnings("unchecked")
        Serializer<Map<String, ?>> serializer = (Serializer<Map<String, ?>>)(Object)new JacksonSerializer<>(objectMapper);
        String token = Jwts.builder()
                .setSubject(username)
                .claim("role", userRole)
                .claim("userId", userId)
                .claim("subscription", user.getSubscriptionType().name())
                .claim("expiry", user.getSubscriptionExpiry())
                .setIssuedAt(currentDate)
                .setExpiration(expireDate)
                .serializeToJsonWith(serializer)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        System.out.println("New token:");
        System.out.println(token);
        return token;
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException("JWT was expired or incorrect", e);
        }
    }
}
