package com.example.users.security;

import com.example.users.entities.User;
import com.example.users.model.SaveToken;
import com.example.users.services.UserService;
import com.example.users.utilities.SessionData;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Slf4j
@Component
@AllArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private JWTUtility jwtHelper;
    private UserDetailsService userDetailsService;
    private UserService userService;
    private SessionData sessionData;


    /** Method to filter the incoming requests
     * @param request: HttpServletRequest -  incoming request
     * @param response: HttpServletResponse - outgoing response
     * @param filterChain: FilterChain - chain of filters to be applied on the request before it reaches the controller
     * @return void
     * */

    @Override
    // this will run before request
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //Header Pattern
        //Authorization = Bearer {token}
        String requestHeader = request.getHeader("Authorization");

        logger.info(" Header :  " + requestHeader);

        String username = null;
        String token = null;

        if (requestHeader != null && requestHeader.startsWith("Bearer")) {

            token = requestHeader.substring(7);
            try {
                username = this.jwtHelper.getUsernameFromToken(token);
            } catch (IllegalArgumentException e) {
                logger.info("Illegal Argument while fetching the username !!");
                e.printStackTrace();
            } catch (ExpiredJwtException e) {
                logger.info("Given jwt token is expired !!");
                e.printStackTrace();
            } catch (MalformedJwtException e) {
                logger.info("Some changed has done in token !! Invalid Token");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        else
        {

            String token1 = null;
            if(sessionData.getUser() != null) {
                 token1 = sessionData.getUser().getToken();
                 log.info("TOKEN FOUND IN SESSION : {}" , token1);
            }

            if(token1!=null)
            {
                token = token1;
                try {
                    username = this.jwtHelper.getUsernameFromToken(token);
                } catch (IllegalArgumentException e) {
                    logger.info("Illegal Argument while fetching the username !!");
                    e.printStackTrace();
                } catch (ExpiredJwtException e) {
                    logger.info("Given jwt token is expired !!");
                    e.printStackTrace();
                } catch (MalformedJwtException e) {
                    logger.info("Some changed has done in token !! Invalid Token");
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
            else
            {
                logger.info("Invalid Header Value !! ");
            }

        }

      //  Authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            //fetch user detail from username
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            Boolean validateToken = this.jwtHelper.validateToken(token, userDetails);
            if (validateToken) {

                //set the authentication
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // If the request does not have Authorization header, add it with the token
                if (requestHeader == null || !requestHeader.startsWith("Bearer")) {
                    request = new CustomHttpServletRequestWrapper(request, "Bearer " + token);
                }
            }
            else
            {
                logger.info("Validation fails !!");
            }
        }
        filterChain.doFilter(request, response);
    }
    private static class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {
        private final String headerValue;

        public CustomHttpServletRequestWrapper(HttpServletRequest request, String headerValue) {
            super(request);
            this.headerValue = headerValue;
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Enumeration<String> headerNames = super.getHeaderNames();
            if (headerNames == null) {
                headerNames = Collections.enumeration(Collections.singleton("Authorization"));
            }
            return headerNames;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singleton(headerValue));
            }
            return super.getHeaders(name);
        }
    }
}
