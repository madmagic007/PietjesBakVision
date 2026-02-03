package me.madmagic.webinterface.socket;

import me.madmagic.detection.VisionRunner;
import me.madmagic.game.GameInstance;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.json.JSONObject;

public class GameSocket implements WebSocketListener {

    private volatile Session s;
    private static String playerName = "";

    @Override
    public void onWebSocketText(String message) {
        if (s == null || !s.isOpen()) return;
        System.out.println(message);

        if (message.equals("request")) {
            JSONObject resp = GameInstance.gameAsJson();
            SessionRegistry.send(s, resp);

            return;
        }

        try {
            JSONObject o = new JSONObject(message);
            playerName = o.optString("playerName");

            switch (o.optString("action")) {
                case "detectionState" -> {
                    boolean state = o.getBoolean("detectionState");

                    if (state) {
                        VisionRunner.start();
                    } else {
                        VisionRunner.stop();
                    }

                    JSONObject resp = new JSONObject()
                            .put("detectionState", !VisionRunner.stopCalled);

                    SessionRegistry.broadcastGames(resp);
                }
                case "join" -> GameInstance.join(playerName);
                case "leave" -> GameInstance.leave(playerName);
                case "newGame" -> GameInstance.newGame(o.optString("newGame", ""));
                case "call" -> GameInstance.nextPlayer(playerName);
                case "stoef" -> GameInstance.stoef(playerName);
                case "accept" -> GameInstance.countThrow(playerName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketConnect(Session session) {
        s = session;
        SessionRegistry.addGameSession(s);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.println("closed: " + reason);
        SessionRegistry.removeSession(s);
        s = null;
    }
}
