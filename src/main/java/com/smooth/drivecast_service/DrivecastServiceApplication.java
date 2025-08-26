package com.smooth.drivecast_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrivecastServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrivecastServiceApplication.class, args);
	}

}
