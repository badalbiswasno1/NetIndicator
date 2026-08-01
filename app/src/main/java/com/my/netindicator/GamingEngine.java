package com.my.netindicator;

import java.util.ArrayList;
import java.util.List;

public class GamingEngine {

    public static class GameRating {
        public String name;
        public String status;
        public int stars;
        public int color;
    }

    public static List<GameRating> analyze(long ping, long jitter, int packetLoss) {
        List<GameRating> list = new ArrayList<>();
        list.add(rate("PUBG Mobile", ping, jitter, packetLoss, 0.50, 1.0, 3.0));
        list.add(rate("Call of Duty Mobile", ping, jitter, packetLoss, 0.65, 1.3, 4.0));
        list.add(rate("Free Fire", ping, jitter, packetLoss, 0.40, 0.8, 2.5));
        return list;
    }

    private static GameRating rate(String name, long ping, long jitter, int loss,
                                     double pingFactor, double jitterFactor, double lossFactor) {
        GameRating g = new GameRating();
        g.name = name;

        if (ping < 0) {
            g.status = "Not Recommended";
            g.stars = 1;
            g.color = 0xFFE63329;
            return g;
        }

        double score = 100 - (ping * pingFactor) - (jitter * jitterFactor) - (loss * lossFactor);
        score = Math.max(0, Math.min(100, score));

        if (score >= 80) {
            g.status = "Playable - Excellent";
            g.color = 0xFF00CC44;
        } else if (score >= 60) {
            g.status = "Good";
            g.color = 0xFF0099FF;
        } else if (score >= 35) {
            g.status = "Poor";
            g.color = 0xFFFFA500;
        } else {
            g.status = "Not Recommended";
            g.color = 0xFFE63329;
        }

        g.stars = Math.max(1, Math.min(5, (int) Math.round(score / 20.0)));
        return g;
    }
}
