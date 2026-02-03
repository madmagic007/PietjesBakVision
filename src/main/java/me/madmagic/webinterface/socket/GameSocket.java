package me.madmagic.webinterface.socket;

import me.madmagic.detection.VisionRunner;
import me.madmagic.game.GameInstance;
import me.madmagic.game.ThrowVal;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                case "newGame" -> GameInstance.newGame();
                case "call" -> GameInstance.nextPlayer(playerName);
                case "stoef" -> GameInstance.stoef(playerName);
                case "accept" -> GameInstance.countThrow(playerName);
                case "customCall" -> {
                    String val = o.getString("customCall");
                    String[] split = val.split(", ");

                    ThrowVal throwVal;

                    if (split.length > 1) {
                        List<Integer> score = Arrays.stream(split)
                                                .map(String::trim)
                                                .map(Integer::parseInt)
                                                .collect(Collectors.toCollection(ArrayList::new));

                        throwVal = ThrowVal.fromScores(score);
                    } else {
                        throwVal = new ThrowVal(ThrowVal.ThrowType.REGULAR, Integer.parseInt(val));
                    }

                    GameInstance.countThrow(playerName, throwVal);
                }
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
        SessionRegistry.removeSession(s);
        s = null;
    }
}
