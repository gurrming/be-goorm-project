package com.example.heartbit.global.jwt;

import com.example.heartbit.global.jwt.dto.IssuedTokens;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.parser.TokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-time}")
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh-token-expiration-time}")
    private long refreshTokenExpirationTime;

    private final SecretKey signingKey;


    private Key getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String id){
        return createToken(id, accessTokenExpirationTime);
    }

    public String createRefreshToken(String id){
        return createToken(id, refreshTokenExpirationTime);
    }

    public String createToken(String id, long expirationTime){
        Claims claims = Jwts.claims().setSubject(id);
        Date now = new Date();
        Date validity = new Date(now.getTime()+expirationTime);

        return Jwts.builder().setClaims(claims).setIssuedAt(now)
                .setExpiration(validity).signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public String getSubject(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(getSignInKey())
                    .build().parseClaimsJws(token);
            return true;
        }catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e){
            log.info("잘못된 JWT 서명입니다.", e);
        }catch (ExpiredJwtException e){
            log.info("만료된 JWT 토큰입니다.",e);
        }catch (UnsupportedJwtException e){
            log.info("지원되지 않는 JWT 토큰입니다.", e);
        }catch(IllegalArgumentException e){
            log.info("JWT 토큰이 잘못되었습니다.", e);
        }
        return false;
    }

    public String newJti(){
        return UUID.randomUUID().toString();
    }

    public IssuedTokens issueNewTokens(long memberId, String deviceId) {
        String jti = newJti();
        Date now = new Date();

        // 1. Access Token 생성
        Date accessTokenExpiresIn = new Date(now.getTime() + accessTokenExpirationTime);
        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setExpiration(accessTokenExpiresIn)
                .setIssuedAt(now)
                .claim("typ", "ACCESS")
                .claim("did", deviceId)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        // 2. Refresh Token 생성
        Date refreshTokenExpiresIn = new Date(now.getTime() + refreshTokenExpirationTime);
        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setExpiration(refreshTokenExpiresIn)
                .setIssuedAt(now)
                .claim("typ", "REFRESH")
                .claim("did", deviceId)
                .claim("jti", jti)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        // 3. 만료 시간(초 단위) 계산
        long accessExpiresInSec = accessTokenExpirationTime / 1000;
        long refreshExpiresInSec = refreshTokenExpirationTime / 1000;

        return new IssuedTokens(accessToken, refreshToken, accessExpiresInSec, refreshExpiresInSec);
    }
}
