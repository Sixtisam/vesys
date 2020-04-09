package ch.fhnw.ds.spark;

import static spark.Spark.get;
import static spark.Spark.port;
//import static spark.Spark.patch;

import java.util.stream.Collectors;

public class HelloWorld {
    public static void main(String[] args) {
    	port(8080);	// default port: 4567
    	
    	get("/hello", "text/plain", (req, res) -> "Hello World1");
    	get("/hello", "text/html", (req, res) -> "<h1>Hello World2</h1>");
        get("/hello", (req, res) -> req.pathInfo());
        
        get("/hello/:name", (req, res) -> "Hello " + req.params(":name"));
        get("say/*/to/*", (req, res) -> "number of splat params: " + req.splat().length);
        get("/redirect", (req, res) -> { res.redirect("/hello"); return null; });
        
        get("/echo", (req, res) -> { 
        	StringBuilder sb = new StringBuilder();
        	sb.append("<b>Headers:</b><br>" + req.headers().stream().map(s -> s + ": " + req.headers(s)).collect(Collectors.joining("\n")) + "<br>");
        	sb.append("<br><b>Query Params:</b><br>" + req.queryParams().stream().map(s -> s + ": " + req.queryParams(s)).collect(Collectors.joining("\n")));
        	return "<pre>" + sb + "</pre>";
        });
    }
}

// curl -X GET -i -H "Accept: text/plain" localhost:8080/hello
// curl -X GET -i -H "Accept: text/html" localhost:8080/hello
// curl -X GET localhost:8080/hello 
// => sends the request with the header Accept: */*


// curl -X GET -i localhost:8080/redirect