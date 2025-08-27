package com.smooth.drivecast_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DrivecastServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrivecastServiceApplication.class, args);
	}

}
