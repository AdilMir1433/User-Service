package com.example.users.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {


    /** Method to create bean of model mapper to later use it for object conversion */
     @Bean
     public ModelMapper modelMapper() {
         ModelMapper modelMapper = new ModelMapper();
         return modelMapper;
     }
}
