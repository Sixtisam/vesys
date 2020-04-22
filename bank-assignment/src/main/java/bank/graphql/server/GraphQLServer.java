package bank.graphql.server;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.kickstart.tools.SchemaParser;
import graphql.schema.GraphQLSchema;

public class GraphQLServer {

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.setConnectors(new Connector[] { connector });
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        servletHandler.addServletWithMapping(BankServlet.class, "/graphql/*").setInitOrder(1); // load on startup
        servletHandler.initialize();
        server.start();
        server.join();
    }

    public static class BankServlet extends GraphQLHttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected GraphQLConfiguration getConfiguration() {
            return GraphQLConfiguration
                    .with(createSchema())
                    .build();
        }

        public GraphQLSchema createSchema() {
            String schemaDesc;
            try {
                schemaDesc = Files.readString(Paths.get(GraphQLServer.class.getResource("bank.graphqls").toURI()));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("failed to read schema file", e);
            }

            GraphQLBank bank = new GraphQLBank();

            return SchemaParser.newParser()
                    .schemaString(schemaDesc)
                    .resolvers(new QueryResolver(bank), new MutationResolver(bank))
                    .build()
                    .makeExecutableSchema();
        }
    }
}