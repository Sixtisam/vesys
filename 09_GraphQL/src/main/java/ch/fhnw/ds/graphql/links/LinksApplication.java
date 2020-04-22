package ch.fhnw.ds.graphql.links;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LinksApplication {

	public static void main(String[] args) {
		// the following property definition is only necessary as this project contains two spring boot applications
		System.setProperty("graphql.tools.schemaLocationPattern", "**/links.graphqls");
		System.setProperty("server.port", "8081");
		SpringApplication.run(LinksApplication.class, args);
	}

}
