package me.madmagic.webinterface;

import jakarta.servlet.http.HttpServlet;
import me.madmagic.webinterface.dests.Home;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ServerInstance {

    private static Server server;

    public static void init(boolean join) throws Exception {
        server = new Server(8080);

        Path jarDir = Paths.get(
                ServerInstance.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParent();
        Path webDir = jarDir.resolve("web");
        String webPath = webDir.toString().replaceAll("build/classes/java/", "");
        System.out.println(webPath);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setResourceBase(webPath);

        dests.forEach(context::addServlet);

        server.setHandler(context);
        server.start();

        if (join) server.join();
    }

    public static void init() throws Exception {
        init(false);
    }

    private static final Map<Class<? extends HttpServlet>, String> dests = new HashMap<>() {{
        put(DefaultServlet.class, "/");
       put(Home.class, "/home");
    }};
}
