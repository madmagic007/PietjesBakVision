package me.madmagic.game;

import org.json.JSONObject;

public class Player {

    public int points, state;
    private ThrowVal highestThisRound;
    public final String name;
    private boolean hasStoeffed = false;
    public boolean hasDecrementedOnce = false;

    public Player(String name) {
        this.name = name;
        newGame();
        roundEnd(false);
    }

    public void newGame() {
        roundEnd(false);

        points = 2;
        state = 0;
    }

    public boolean roundEnd(boolean won) {
        int pointsToSubtract = 0;

        if (won) {
            pointsToSubtract = highestThisRound.type().rank;
        }

        if (hasStoeffed) {
            if (won) {
                pointsToSubtract *= 2;
            } else {
                pointsToSubtract = -1;
            }
        }

        highestThisRound = ThrowVal.blank;
        hasStoeffed = false;

        return subtractPoints(pointsToSubtract);
    }

    public void checkThrow(ThrowVal throwVal) {
        if (throwVal.higherThan(highestThisRound))
            highestThisRound = throwVal;
    }

    public boolean subtractPoints(int amt) {
        hasDecrementedOnce = amt > 0;
        points -= amt;

        if (points <= 0) {
            points = 0;
            return true;
        }
        return false;
    }

    void stoef() {
        hasStoeffed = true;
    }

    public JSONObject dataAsJson() {
        return new JSONObject()
                .put("name", name)
                .put("points", points)
                .put("state", state)
                .put("hasStoeffed", hasStoeffed)
                .put("highestThisRound", highestThisRound.scoreAsString());
    }
}
