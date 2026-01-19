package me.madmagic.webinterface;

import jakarta.servlet.http.HttpServlet;
import me.madmagic.webinterface.socket.GameSocket;
import me.madmagic.webinterface.socket.SettingsSocket;
import me.madmagic.webinterface.socket.VideoSocket;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

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

        JettyWebSocketServletContainerInitializer.configure(
                context, (sc, c) -> {
                    c.addMapping("/ws/settings", (_, _) -> new SettingsSocket());
                    c.addMapping("/ws/video", (_, _) -> new VideoSocket());
                    c.addMapping("/ws/game", (_, _) -> new GameSocket());
                }
        );

        dests.forEach((d, c) -> context.addServlet(c, d));

        server.setHandler(context);
        server.start();

        if (join) server.join();
    }

    public static void init() throws Exception {
        init(false);
    }

    private static final Map<String, Class<? extends HttpServlet>> dests = new HashMap<>() {{
        put("/", DefaultServlet.class);
        put("/settings", DefaultServlet.class);
    }};
}
