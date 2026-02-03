package me.madmagic.game;

import me.madmagic.detection.VisionRunner;
import me.madmagic.webinterface.socket.SessionRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameInstance {

    private static final List<Player> players = new ArrayList<>();

    private static int maxThrowsThisRound = 3,
                        curThrowCount = 1,
                        curPlayerIndex = -1,
                        roundStartIndex = -1,
                        winningPlayerIndex = -1;

    private static ThrowVal highestThisRound = ThrowVal.blank;

    public static void newGame(String startingPlayer) {
        if (!startingPlayer.isEmpty()) {
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                if (p.name.equals(startingPlayer)) {
                    roundStartIndex = i;
                    curPlayerIndex = i;
                }
            }
        }

        if (roundStartIndex == -1) {
            roundStartIndex = 0;
            curPlayerIndex = 0;
        }

        players.forEach(Player::newGame);
        newRound();

        broadcastGame();
    }

    public static void newRound() {
        maxThrowsThisRound = 3;
        curThrowCount = 1;
        winningPlayerIndex = -1;
        highestThisRound = ThrowVal.blank;
    }

    public static void roundEnd() {
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            p.roundEnd(winningPlayerIndex == i);
        }

        curPlayerIndex = winningPlayerIndex;
        roundStartIndex = winningPlayerIndex;
        newRound();
    }

    private static boolean playerExists(String playerName) {
        for (Player player : players) {
            if (player.name.equals(playerName)) return true;
        }

        return false;
    }

    public static void nextPlayer() {
        System.out.println("next player");

        curPlayerIndex = (curPlayerIndex +1) % players.size();
        curThrowCount = 1;

        if (curPlayerIndex == roundStartIndex) {
            roundEnd();
        }
    }

    public static void join(String playerName) {
        if (playerExists(playerName)) return;

        players.add(new Player(playerName));

        broadcastGame();
    }

    public static void leave(String playerName) {
        int index = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(playerName)) {
                index = i;
                break;
            }
        }

        if (index < 0) return;

        players.remove(index);
        broadcastGame();
    }

    public static void nextPlayer(String playerName) { // call on interface
        maxThrowsThisRound = curThrowCount;
        countThrow(playerName);
    }

    public static void countThrow(String playerName) {
        ThrowVal throwVal = VisionRunner.getCurrentThrow();
        countThrow(playerName, throwVal);
    }

    public static void countThrow(String playerName, ThrowVal throwVal) {
        if (curThrowCount > maxThrowsThisRound ||
                !players.get(curPlayerIndex).name.equals(playerName)) return;

        if (throwVal.higherThan(highestThisRound)) {
            winningPlayerIndex = curPlayerIndex;
            highestThisRound = throwVal;
        }

        players.get(curPlayerIndex).checkThrow(throwVal);


        curThrowCount++;

        if (curThrowCount > maxThrowsThisRound)
            nextPlayer();

        broadcastGame();
    }

    public static void stoef(String playerName) {
        if (curThrowCount != 1 ||
                !players.get(curPlayerIndex).name.equals(playerName)) return;

        players.get(curPlayerIndex).stoef();
        maxThrowsThisRound = 1;
        countThrow(playerName);
    }

    public static void broadcastGame() {
        JSONObject o = gameAsJson();
        SessionRegistry.broadcastGames(o);
    }

    public static JSONObject gameAsJson() {
        JSONArray pA = new JSONArray();

        players.forEach(p -> {
            JSONObject data = p.dataAsJson();
            pA.put(data);
        });

        JSONObject o = new JSONObject()
                .put("highestThisRound", highestThisRound.scoreAsString())
                .put("curThrowCount", curThrowCount)
                .put("maxThrowsThisRound", maxThrowsThisRound)
                .put("detectionState", VisionRunner.isRunning())
                .put("players", pA);

        if (!players.isEmpty() ) {
            if (winningPlayerIndex >= 0) {
                o.put("winningPlayer", players.get(winningPlayerIndex).name);
            }

            if (curPlayerIndex >= 0) {
                o.put("curPlayer", players.get(curPlayerIndex).name);
            }
        }

        return o;
    }
}
