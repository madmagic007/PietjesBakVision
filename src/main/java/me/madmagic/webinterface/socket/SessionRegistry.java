package me.madmagic.webinterface.socket;

import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    private static final Set<Session> settingSessions = ConcurrentHashMap.newKeySet();
    private static final Set<Session> videoSessions = ConcurrentHashMap.newKeySet();
    private static final Set<Session> gameSessions = ConcurrentHashMap.newKeySet();

    public static void broadcastSettings(JSONObject settings) {
        broadcast(settingSessions, settings.toString());
    }

    public static void broadcastGames(JSONObject gameData) {
        broadcast(gameSessions, gameData.toString());
    }

    public static void broadcastImages() {
        videoSessions.forEach(s -> {
            try {
                //s.getRemote().sendBytes();
            } catch (Exception ignored) {
                s.close();
            }
        });
    }

    private static void broadcast(Set<Session> sessions, String text) {
        sessions.forEach(s -> {
            try {
                s.getRemote().sendString(text);
            } catch (Exception ignored) {
                s.close();
            }
        });
    }

    public static void addSettingSession(Session session) {
        settingSessions.add(session);
    }

    public static void addVideoSession(Session session) {
        videoSessions.add(session);
    }

    public static void addGameSession(Session session) {
        gameSessions.add(session);
    }

    public static void removeSession(Session session) {
        settingSessions.remove(session);
        videoSessions.remove(session);
        gameSessions.remove(session);
    }
}
