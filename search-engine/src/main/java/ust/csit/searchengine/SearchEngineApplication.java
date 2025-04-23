package ust.csit.searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchEngineApplication {

	public static void main(String[] args) {
		System.out.println("=======Successfully started=======");
		SpringApplication.run(SearchEngineApplication.class, args);
	}

}
