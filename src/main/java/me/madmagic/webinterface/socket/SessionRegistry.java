package me.madmagic.webinterface.socket;

import me.madmagic.Util;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.UMat;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;

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

    public static void broadcastMats(UMat... mats) {
        if (videoSessions.isEmpty()) return;

        List<byte[]> images = new ArrayList<>();
        int totalSize = 4;

        for (UMat mat : mats) {
            UMat scaled = new UMat();
            Util.imScale(mat, scaled, 0.2);

            BytePointer buf = new BytePointer();
            imencode(".jpg", scaled, buf);

            byte[] imgBytes = new byte[(int) buf.limit()];
            buf.get(imgBytes);
            buf.deallocate();

            images.add(imgBytes);
            totalSize += 4 + imgBytes.length;

            scaled.close();
        }

        ByteBuffer packet = ByteBuffer.allocate(totalSize);
        packet.putInt(mats.length);

        for (byte[] imgBytes : images) {
            packet.putInt(imgBytes.length);
            packet.put(imgBytes);
        }

        packet.flip();

        videoSessions.forEach(s -> {
            try {
                s.getRemote().sendBytes(packet);
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
