package ch.fhnw.ds.rest;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
//      register(UserResource.class);
		packages("ch.fhnw.ds.rest");
		register(OpenApiResource.class);
	}

}