package com.auth_service.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Long id, String username, String role, Long managerId, String kycStatus, Long hrId) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withSubject(username)
                .withClaim("id", id)
                .withClaim("role", role)
                .withClaim("managerId", managerId != null ? managerId : 0L)
                .withClaim("kycStatus", kycStatus);

        if (hrId != null) {
            builder = builder.withClaim("hrId", hrId);
        }

        return builder.withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(algorithm);
    }
}
