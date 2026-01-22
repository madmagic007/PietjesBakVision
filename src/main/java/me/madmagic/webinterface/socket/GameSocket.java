package me.madmagic.webinterface.socket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

public class GameSocket implements WebSocketListener {

    private volatile Session s;

    @Override
    public void onWebSocketText(String message) {
        if (s == null || !s.isOpen()) return;

        System.out.println(message);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        s = session;
        SessionRegistry.addGameSession(s);
        System.out.println("open");
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        SessionRegistry.removeSession(s);
        System.out.println("close");
        s = null;
    }
}
