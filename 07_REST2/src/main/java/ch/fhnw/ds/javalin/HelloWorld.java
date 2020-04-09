package ch.fhnw.ds.javalin;
import io.javalin.Javalin;

public class HelloWorld {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/hello/", ctx -> ctx.result("Hello World"));
        app.get("/hello/:name", ctx -> ctx.result("Hello " + ctx.pathParam("name")));
    }

}

