package me.madmagic.game;

import me.madmagic.detection.VisionRunner;

import java.util.LinkedHashMap;
import java.util.Map;

public class GameInstance {

    private static final Map<String, Player> players = new LinkedHashMap<>();

    private static int roundHighest, roundMaxThrows, curThrow, player;

    public static void newGame() {
        players.forEach((s, p) -> p.resetPoints());
        newRound();

        player = 0;
    }

    public static void newRound() {
        players.forEach((s, p) -> p.newRound());

        roundHighest = 0;
        roundMaxThrows = 3;
        curThrow = 1;

        player = 0;
    }

    public static void nextPlayer() {
        player++;

        if (player == players.size()) {
            newRound();
        }
    }

    public static void join(String playerName) {
        if (players.containsKey(playerName)) return;

        players.put(playerName, new Player());
        //todo broadcast
    }

    public static void leave(String playerName) {
        if (!players.containsKey(playerName)) return;

        players.remove(playerName);
        //todo broadcast
    }

    public static void call(String playerName) {
        if (curThrow == 3 || curThrow > roundMaxThrows) return;

        ThrowVal throwVal = VisionRunner.getCurrentThrow();

        curThrow++;
    }

    public static void stoef(String playerName) {
        if (curThrow != 1) return;

    }
}
