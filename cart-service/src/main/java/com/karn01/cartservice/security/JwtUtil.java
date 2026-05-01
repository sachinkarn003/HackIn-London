//package com.karn01.cartservice.security;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.security.Key;
//
//@Component
//public class JwtUtil {
//    private static final String SECRET =
//            "this-is-my-super-secret-key-which-is-very-long-123456";;
//    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
//
//    public String extractUserId(String token){
//        return Jwts.parser()
//                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
//                .build()
//                .parseSignedClaims(token)
//                .getPayload()
//                .getSubject();
//    }
//
//}
