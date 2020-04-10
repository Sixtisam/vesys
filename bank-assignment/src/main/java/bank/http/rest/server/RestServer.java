package bank.http.rest.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import bank.InactiveException;
import bank.OverdrawException;
import bank.http.rest.ExceptionDTO;

public class RestServer {
    public static void main(String[] args) throws URISyntaxException {
        URI baseUri = new URI("http://localhost:8080/");

        // @Singleton annotations will be respected
        ResourceConfig rc = new ResourceConfig(
                BankResource.class,
                IllegalArgumentExceptionMapper.class,
                OverdrawExceptionMapper.class,
                ResponseLoggingFilter.class,
                InactiveExceptionMapper.class);
        // Create and start the JDK HttpServer with the Jersey application
        JdkHttpServerFactory.createHttpServer(baseUri, rc);
        System.out.println("BankRestServer is up & running!");
    }

    /**
     * Logs the requests
     */
    @Provider
    public static class ResponseLoggingFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            System.out.println(requestContext.getMethod().toUpperCase() + ": "
                    + requestContext.getUriInfo().getRequestUri().toString() + " --> " + responseContext.getStatus());
        }
    }

    @Provider
    public static class InactiveExceptionMapper implements ExceptionMapper<InactiveException> {
        @Override
        public Response toResponse(InactiveException exception) {
            return Response.status(Status.GONE).build();
        }
    }

    @Provider
    public static class OverdrawExceptionMapper implements ExceptionMapper<OverdrawException> {
        @Override
        public Response toResponse(OverdrawException exception) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ExceptionDTO(exception))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    @Provider
    public static class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
        @Override
        public Response toResponse(IllegalArgumentException exception) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ExceptionDTO(exception))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }
}
