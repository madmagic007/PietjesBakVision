package me.madmagic.webinterface.socket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

public class VideoSocket implements WebSocketListener {

    private volatile Session s;

    @Override
    public void onWebSocketText(String message) {
        if (message.equals("ping")) {
            SessionRegistry.send(s, "pong");
        }
    }

    @Override
    public void onWebSocketConnect(Session session) {
        s = session;
        SessionRegistry.addVideoSession(s);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        SessionRegistry.removeSession(s);
        s = null;
    }
}
