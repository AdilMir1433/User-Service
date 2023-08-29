package com.example.users;

import com.example.users.utilities.SessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UsersApplication {

	@Autowired
	SessionData sessionData;
	public static void main(String[] args) {
		SpringApplication.run(UsersApplication.class, args);
	}
	CommandLineRunner commandLineRunner = new CommandLineRunner() {
		@Override
		public void run(String... args) throws Exception {
			sessionData.removeSessionData();
		}
	};

}
