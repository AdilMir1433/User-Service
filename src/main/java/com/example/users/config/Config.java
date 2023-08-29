package com.example.users.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class Config {


    /** Method to create a bean of password encoder to later save passwords in database encoded */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration builder) throws Exception {
        return builder.getAuthenticationManager();
    }
    @Bean
    public Cloudinary getCloudinary(){

        Map config = new HashMap<>();
        config.put("cloud_name", "dn9ugl96j");
        config.put("api_key", "618395338318682");
        config.put("api_secret", "1gohwcpUwsExaRv1Xyz7g7Gz8Bo");

        return new Cloudinary(config);
    }

}
