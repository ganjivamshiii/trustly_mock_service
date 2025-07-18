package com.cpt.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class TrustlyMockServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrustlyMockServiceApplication.class, args);
		log.info("Trustly Mock Service Application started successfully.");
	}

}
