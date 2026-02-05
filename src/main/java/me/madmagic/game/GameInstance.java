package me.madmagic.game;

import me.madmagic.detection.VisionRunner;
import me.madmagic.webinterface.socket.SessionRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameInstance {

    private static final List<Player> players = new ArrayList<>();
    private static final List<Player> finishedPlayers = new ArrayList<>();
    private static final List<Player> playingPlayers = new ArrayList<>();
//    private static final List<Player> players = Arrays.asList(
//            new Player("Diego"),
//            new Player("Seth"),
//            new Player("Noa"),
//            new Player("Wout")
//    );

    private static int maxThrowsThisRound = 3,
                        curThrowCount = 1,
                        curPlayerIndex = -1,
                        roundStartIndex = -1,
                        winningPlayerIndex = -1;

    private static ThrowVal highestThisRound = ThrowVal.blank;

    public static void newGame(String startingPlayer) {
        playingPlayers.clear();
        playingPlayers.addAll(players);

        if (!startingPlayer.isEmpty()) {
            for (int i = 0; i < playingPlayers.size(); i++) {
                Player p = playingPlayers.get(i);

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

        finishedPlayers.clear();
        playingPlayers.forEach(Player::newGame);

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
        for (int i = 0; i < playingPlayers.size(); i++) {
            Player p = playingPlayers.get(i);

            boolean finished = p.roundEnd(winningPlayerIndex == i);

            if (finished) {
                p.state = finishedPlayers.size() +1;
                finishedPlayers.add(p);
            }
        }

        playingPlayers.removeAll(finishedPlayers);

        curPlayerIndex = (winningPlayerIndex) % playingPlayers.size();
        roundStartIndex = curPlayerIndex;

        newRound();
    }

    private static boolean playerExists(String playerName) {
        for (Player player : players) {
            if (player.name.equals(playerName)) return true;
        }

        return false;
    }

    public static void nextPlayer(boolean noAdd) {
        int beforeIndex = curPlayerIndex;

        if (!noAdd) curPlayerIndex = (curPlayerIndex +1) % playingPlayers.size();
        curThrowCount = 1;

        if (curPlayerIndex == roundStartIndex && beforeIndex != curPlayerIndex) {
            roundEnd();
        }
    }

    public static void join(String playerName) {
        if (playerExists(playerName)) return;

        Player p = new Player(playerName);
        players.add(p);

        if (curPlayerIndex != -1) playingPlayers.add(p);

        broadcastGame();
    }

    public static void leave(String playerName) {
        Player p = null;

        for (Player pl : players) {
            if (pl.name.equals(playerName)) {
                p = pl;
                break;
            }
        }

        if (p == null) return;

        players.remove(p);
        playingPlayers.remove(p);

        broadcastGame();
    }

    public static void acceptThrow(String playerName) {
        ThrowVal throwVal = VisionRunner.getCurrentThrow();

        if (countThrow(playerName, throwVal) == 1) {
            maxThrowsThisRound = curThrowCount;
            afterThrow();
        }
    }

    public static void stoef(String playerName) {
        if (curThrowCount != 1) return;

        ThrowVal throwVal = VisionRunner.getCurrentThrow();

        if (countThrow(playerName, throwVal) == 1) {
            playingPlayers.get(curPlayerIndex).stoef();
            maxThrowsThisRound = 1;

            afterThrow();
        }
    }

    public static void countThrow(String playerName) {
        ThrowVal throwVal = VisionRunner.getCurrentThrow();

        if (countThrow(playerName, throwVal) == 1) {
            afterThrow();
        }
    }

    public static int countThrow(String playerName, ThrowVal throwVal) {
        if (curThrowCount > maxThrowsThisRound ||
                !playingPlayers.get(curPlayerIndex).name.equals(playerName)) return 0;

        Player p = playingPlayers.get(curPlayerIndex);

        if (throwVal.type().equals(ThrowVal.ThrowType.DRIE_APEN)) {
            if (p.hasDecrementedOnce) {
                p.state = finishedPlayers.size() + 1;
            } else {
                p.state = -1;
            }

            p.points = 0;
            playingPlayers.remove(p);

            nextPlayer(true);
            broadcastGame();

            return -1;
        }

        if (throwVal.higherThan(highestThisRound)) {
            winningPlayerIndex = curPlayerIndex;
            highestThisRound = throwVal;
        }

        p.checkThrow(throwVal);

        return 1;
    }

    private static void afterThrow() {
        curThrowCount++;
        if (curThrowCount > maxThrowsThisRound)
            nextPlayer(false);

        broadcastGame();
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
                o.put("winningPlayer", playingPlayers.get(winningPlayerIndex).name);
            }

            if (curPlayerIndex >= 0) {
                o.put("curPlayer", playingPlayers.get(curPlayerIndex).name);
            }
        }

        return o;
    }
}
