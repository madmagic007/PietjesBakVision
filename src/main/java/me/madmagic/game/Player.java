package me.madmagic.game;

import org.json.JSONObject;

public class Player {

    private int points;
    private ThrowVal highestThisRound;
    public final String name;
    private boolean hasStoeffed = false,
                    hasDecrementedOnce = false;

    public Player(String name) {
        this.name = name;
        newGame();
        roundEnd(false);
    }

    public void newGame() {
        points = 9;
    }

    public void roundEnd(boolean won) {
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

        points -= pointsToSubtract;
        highestThisRound = ThrowVal.blank;
        hasStoeffed = false;
    }

    public void checkThrow(ThrowVal throwVal) {
        if (throwVal.higherThan(highestThisRound))
            highestThisRound = throwVal;
    }

    public boolean subtractPoints(int amt) {
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

    boolean shouldThrow() {
        return points > 0;
    }

    public JSONObject dataAsJson() {
        return new JSONObject()
                .put("name", name)
                .put("points", points)
                .put("hasStoeffed", hasStoeffed)
                .put("highestThisRound", highestThisRound.scoreAsString());
    }
}
