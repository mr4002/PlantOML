package com.plantoml.plantomlserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the PlantOML Web Server application.
 * <p>
 * This class bootstraps and launches the Spring Boot application. It is responsible
 * for initializing the application context and setting up the server.
 */
@SpringBootApplication
public class PlantOmlWebServerApplication {

	/**
	 * Main method which serves as the entry point for the Spring Boot application.
	 * <p>
	 * This method launches the Spring Boot application by initializing the
	 * application context and starting the embedded server.
	 *
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(PlantOmlWebServerApplication.class, args);
	}
}