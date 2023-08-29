package com.example.users.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

//TODO: Helper Class
@Component
public class JWTUtility {
    public static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60; // 5 Hours represented in seconds

    private String secret = "afafasfafafasfasfasfafacasdasfasxASFACASDFACASDFASFASFDAFASFASDAADSCSDFADCVSGCFVADXCcadwavfsfarvf";

    //retrieve username from jwt token

    /** Retrieves Username from given token
     *  @param token - JWT Token to be decoded - Type String
     *  @return String - Username
     * */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }


    /** Method to get the expiry date of token
     * @param token - JWT Token to be decoded - Type String
     * @return Date - Expiration Date of token
     * */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /** Method to get the claim from token
     * @param token - JWT Token to be decoded - Type String
     * @param claimsResolver - Function to get the claim from token
     * @return T - Claim from token
     * */

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }


    /** Method to get all the claims of a particular user from its token
     * @param token - JWT Token to be decoded - Type String
     * @return Claims - All the claims of a particular user from its token
     * */

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }


    /** Method to check if the token has expired or not
     * @param token - JWT Token to be decoded - Type String
     * @return Boolean - True if token has expired, else False
     * */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }


    /** Method to generate a token of a user for
     * @param userDetails - User for which token is to be generated : Type UserDetails
     * @return String - Token generated for the user
     * */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, userDetails.getUsername());
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {

        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000)) // convert seconds to milliseconds
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    //validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

}
