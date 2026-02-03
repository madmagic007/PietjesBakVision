package me.madmagic.game;

import java.util.HashSet;
import java.util.List;

public record ThrowVal(ThrowType type, int summedScore) implements Comparable<ThrowVal> {

    public static final ThrowVal blank = new ThrowVal(ThrowType.REGULAR, 0);

    public String scoreAsString() {
        switch (type) {
            case REGULAR -> {
                return String.valueOf(summedScore);
            }
            case ZAND -> {
                return "Zand van " + summedScore;
            }
            case SOIX -> {
                return "Soix";
            }
            case DRIE_APEN -> {
                return "Drie Apen";
            }
            default -> {
                return "";
            }
        }
    }

    @Override
    public int compareTo(ThrowVal other) {
        int typeCompare = Integer.compare(type.getRank(), other.type.getRank());
        if (typeCompare != 0) return typeCompare;
        return Integer.compare(summedScore, other.summedScore);
    }

    public boolean higherThan(ThrowVal other) {
        return compareTo(other) > 0;
    }

    public static ThrowVal fromScores(List<Integer> scores) {
        if (scores.size() != 3) return null;

        List<Integer> sorted = scores.stream().sorted().toList();
        HashSet<Integer> unique = new HashSet<>(scores);

        ThrowType type = ThrowType.REGULAR;
        int score = 0;

        if (unique.size() == 1) {
            if (sorted.getFirst() == 1) {
                type = ThrowType.DRIE_APEN;
            } else {
                type = ThrowType.ZAND;
                score = sorted.getFirst();
            }
        } else if (sorted.equals(List.of(4, 5, 6))) {
            type = ThrowType.SOIX;
        } else {
            for (Integer i : scores) {
                if (i == 1) score += 100;
                else if (i == 6) score += 60;
                else score += i;
            }
        }

        return new ThrowVal(type, score);
    }

    public enum ThrowType {
        REGULAR(1),
        ZAND(2),
        SOIX(3),
        DRIE_APEN(4);

        public final int rank;

        ThrowType(int rank) {
            this.rank = rank;
        }

        public int getRank() {
            return rank;
        }
    }
}
