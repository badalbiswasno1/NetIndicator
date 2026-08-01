package com.my.netindicator;

import java.util.ArrayList;
import java.util.List;

public class NetworkScoreEngine {

    public static class Result {
        public int score;
        public String category;
        public int categoryColor;
        public List<String> reasons;
        public String exactGrade;
    }

    public static Result compute(double baseGrade, int signalDbm, int rsrq, int sinr,
                                   long pingMs, boolean carrierAggregation, boolean hasSinr, boolean isWifi) {
        Result r = new Result();
        r.reasons = new ArrayList<>();

        double signalScore = signalSubScore(signalDbm);
        double pingScore = pingSubScore(pingMs);
        double qualityScore = hasSinr ? qualitySubScore(rsrq, sinr) : signalScore;
        double typeScore = (baseGrade / 5.0) * 100.0;

        double total;
        if (isWifi) {
            total = pingScore * 0.75 + 100 * 0.25;
            r.reasons.add("Connected via WiFi");
        } else {
            total = signalScore * 0.35 + pingScore * 0.30 + qualityScore * 0.20 + typeScore * 0.15;
            if (carrierAggregation) {
                total += 5;
                r.reasons.add("Carrier aggregation detected");
            }
            if (signalDbm == 0) {
                total = Math.min(total, 40);
            }
        }

        total = Math.max(0, Math.min(100, total));
        r.score = (int) Math.round(total);

        if (r.score >= 85) {
            r.category = "EXCELLENT";
            r.categoryColor = 0xFF00CC44;
        } else if (r.score >= 65) {
            r.category = "GOOD";
            r.categoryColor = 0xFF0099FF;
        } else if (r.score >= 40) {
            r.category = "FAIR";
            r.categoryColor = 0xFFFFA500;
        } else {
            r.category = "POOR";
            r.categoryColor = 0xFFE63329;
        }

        if (!isWifi && signalDbm != 0) {
            if (signalDbm > -80) r.reasons.add("Strong signal");
            else if (signalDbm > -100) r.reasons.add("Moderate signal");
            else r.reasons.add("Weak signal");
        }

        if (pingMs < 0) {
            r.reasons.add("No response from server");
        } else if (pingMs < 40) {
            r.reasons.add("Very low latency");
        } else if (pingMs < 100) {
            r.reasons.add("Low latency");
        } else if (pingMs < 200) {
            r.reasons.add("Moderate latency");
        } else {
            r.reasons.add("High latency");
        }

        double exact = baseGrade + (signalScore / 100.0) * 0.9;
        double maxGrade = baseGrade + 0.9;
        if (exact > maxGrade) exact = maxGrade;
        if (baseGrade <= 0) {
            r.exactGrade = "?.0G";
        } else {
            r.exactGrade = String.format("%.1fG", exact);
        }

        return r;
    }

    private static double signalSubScore(int dbm) {
        if (dbm == 0) return 0;
        int clamped = Math.max(-120, Math.min(-50, dbm));
        return ((clamped + 120) / 70.0) * 100.0;
    }

    private static double pingSubScore(long ping) {
        if (ping < 0) return 0;
        if (ping <= 20) return 100;
        if (ping >= 400) return 0;
        return 100.0 - ((ping - 20) / 380.0) * 100.0;
    }

    private static double qualitySubScore(int rsrq, int sinr) {
        double rsrqScore = rsrq == 0 ? 70 : Math.max(0, Math.min(100, ((rsrq + 20) / 14.0) * 100.0));
        double sinrScore = sinr == 0 ? 70 : Math.max(0, Math.min(100, (sinr / 30.0) * 100.0));
        return (rsrqScore + sinrScore) / 2.0;
    }
}
