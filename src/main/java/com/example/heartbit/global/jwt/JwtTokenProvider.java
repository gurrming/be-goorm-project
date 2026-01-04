package com.example.heartbit.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-time}")
    private long expirationTime;


    private Key getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String id){
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
}
