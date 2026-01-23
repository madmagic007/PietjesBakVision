package me.madmagic.game;

import org.json.JSONObject;

public class Player {

    private int points, highestThisRound;

    public Player() {
        resetPoints();
    }

    public void resetPoints() {
        points = 9;
    }

    public void newRound() {
        highestThisRound = 0;
    }

    public boolean subtractPoints(int amt) {
        points -= amt;

        if (points <= 0) {
            points = 0;
            return true;
        }
        return false;
    }

    boolean shouldThrow() {
        return points > 0;
    }

    public JSONObject dataAsJson() {
        return new JSONObject()
                .put("points", points)
                .put("highestThisRound", highestThisRound);
    }
}
