package tomcat;

import java.io.File;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

// Starts a tomcat server with a single web app accessible at http://localhost:8080/ds
public class StartServer {

    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
		tomcat.setBaseDir("build");

        // adds a webapp directory under a particular root name
        String webappDirLocation = "src/main/webapp/";
        StandardContext ctx = (StandardContext) tomcat.addWebapp("/ds", new File(webappDirLocation).getAbsolutePath());

        // adds  an alternative location for your "WEB-INF/classes" directory
        File additionWebInfClasses = new File("build/classes/java/main");
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(
        		new DirResourceSet(
        				resources, 
        				"/WEB-INF/classes",	// /WEB-INF/lib/ for jars
        				 additionWebInfClasses.getAbsolutePath(), 
        				 "/"));
        ctx.setResources(resources);

		tomcat.getConnector();	// creates the default connector
        tomcat.start();
        tomcat.getServer().await();
    }
}