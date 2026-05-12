package me.madmagic.webinterface;

import me.madmagic.webinterface.socket.BleSettingsSocket;
import me.madmagic.webinterface.socket.GameSocket;
import me.madmagic.webinterface.socket.VideoSocket;
import me.madmagic.webinterface.socket.VisionSettingsSocket;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        context.setWelcomeFiles(new String[] { "index.html" });
        context.addServlet(DefaultServlet.class, "/");

        JettyWebSocketServletContainerInitializer.configure(
                context, (sc, c) -> {
                    c.addMapping("/ws/visionSettings", (_, _) -> new VisionSettingsSocket());
                    c.addMapping("/ws/bleSettings", (_, _) -> new BleSettingsSocket());
                    c.addMapping("/ws/video", (_, _) -> new VideoSocket());
                    c.addMapping("/ws/game", (_, _) -> new GameSocket());
                }
        );

        server.setHandler(context);
        server.start();

        if (join) server.join();
    }

    public static void init() throws Exception {
        init(false);
    }
}
