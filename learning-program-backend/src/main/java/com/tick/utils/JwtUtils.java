package com.tick.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {

    @Value("${spring.security.jwt.key}")
    String key;

    @Value("${spring.security.jwt.expire}")
    int expire;

    public boolean validateToken(String headerToken) {
        String token = this.convertToken(headerToken);
        if(token == null){
            return false;
        }
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            int id=jwt.getId();
        }catch (JWTVerificationException exception){
            return false;
        }
    }

    public boolean deleteToken(String uuid, Date expiration) {
        String token = this.convertToken(uuid);
    }

    private boolean isInvalidToken(String token) {

    }

    public DecodedJWT resolveJwt(String headerToken){
        String token = this.convertToken(headerToken);
        if(token == null){
            return null;
        }
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT decodedJWT = jwtVerifier.verify(token);
            Date expiresAt = decodedJWT.getExpiresAt();
            return new Date().after(expiresAt) ? null : decodedJWT;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public String createJwt(UserDetails details,int id,String username){
        Algorithm algorithm = Algorithm.HMAC256(key);
        Date expire = this.expireTime();
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("id",id)
                .withClaim("username",username)
                .withClaim("authorities",details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .withExpiresAt(expire)
                .withIssuedAt(new Date())
                .sign(algorithm);
    }

    public Date expireTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire);
        return calendar.getTime();
    }

    public UserDetails toUser(DecodedJWT jwt){
        Map<String, Claim> claims =  jwt.getClaims();
        return User.withUsername(claims.get("username").asString())
                .password("******")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    public Integer toId(DecodedJWT jwt){
        Map<String, Claim> claims =  jwt.getClaims();
        return claims.get("id").asInt();
    }

    private String convertToken(String token){
        if(token == null || !token.startsWith("Bearer ")){
            return null;
        }
        return token.substring(7);
    }
}
