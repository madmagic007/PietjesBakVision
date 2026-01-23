package me.madmagic.game;

import java.util.List;

public class ThrowVal {

    public static ThrowVal fromScores(List<Integer> scores) {
        return new ThrowVal();
    }

    public enum ThrowType {
        REGULAR,
        ZAND,
        SOIX,
        DRIE_APEM;
    }
}
