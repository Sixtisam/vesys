package tomcat;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

// Starts a single server accessible at http://localhost:8080/simple/go
public class StartServlet {

	public static void main(String[] args) throws LifecycleException {
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(8080);
		tomcat.setBaseDir("build");

		@SuppressWarnings("serial")
		HttpServlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();
				writer.println("<html><title>Welcome</title><body>");
				writer.println("<h1>Have a Great Day!</h1>");
				writer.println("</body></html>");
			}
		};

		// Analogous to the definition in web.xml
		String contextPath = "/simple";
		String servletName = "SimpleServlet";
		String urlPattern = "/go";
		Context context = tomcat.addContext(contextPath, new File(".").getAbsolutePath());
		tomcat.addServlet(contextPath, servletName, servlet);
		context.addServletMappingDecoded(urlPattern, servletName);

		tomcat.getConnector();	// creates the default connector
		tomcat.start();
		tomcat.getServer().await();
	}
}
