package com.my.netindicator;

import org.json.JSONArray;
import org.json.JSONObject;

public class StabilityEngine {

    public static class Result {
        public int score;
        public String label;
        public int labelColor;
        public double pingStdDev;
        public int dropCount;
        public int gradeChanges;
    }

    public static Result analyze(JSONArray logs, int windowSize) {
        Result r = new Result();
        try {
            int start = Math.max(0, logs.length() - windowSize);
            int n = logs.length() - start;
            if (n <= 1) {
                r.score = 50;
                r.label = "Not enough data";
                r.labelColor = 0xFF888888;
                return r;
            }

            double sum = 0;
            int validCount = 0;
            int drops = 0;
            String lastGrade = null;
            int gradeChanges = 0;

            for (int i = start; i < logs.length(); i++) {
                JSONObject obj = logs.getJSONObject(i);
                long ping = obj.getLong("ping");
                if (ping < 0) {
                    drops++;
                } else {
                    sum += ping;
                    validCount++;
                }
                String grade = obj.getString("grade");
                if (lastGrade != null && !lastGrade.equals(grade)) {
                    gradeChanges++;
                }
                lastGrade = grade;
            }

            double mean = validCount > 0 ? sum / validCount : 0;
            double varSum = 0;
            for (int i = start; i < logs.length(); i++) {
                JSONObject obj = logs.getJSONObject(i);
                long ping = obj.getLong("ping");
                if (ping >= 0) {
                    varSum += Math.pow(ping - mean, 2);
                }
            }
            double stdDev = validCount > 0 ? Math.sqrt(varSum / validCount) : 0;

            r.pingStdDev = stdDev;
            r.dropCount = drops;
            r.gradeChanges = gradeChanges;

            double score = 100 - (stdDev * 1.2) - (drops * 12) - (gradeChanges * 4);
            r.score = (int) Math.max(0, Math.min(100, Math.round(score)));

            if (r.score >= 85) {
                r.label = "Excellent";
                r.labelColor = 0xFF00CC44;
            } else if (r.score >= 65) {
                r.label = "Good";
                r.labelColor = 0xFF0099FF;
            } else if (r.score >= 40) {
                r.label = "Average";
                r.labelColor = 0xFFFFA500;
            } else {
                r.label = "Poor";
                r.labelColor = 0xFFE63329;
            }
        } catch (Exception e) {
            r.score = 0;
            r.label = "Error";
            r.labelColor = 0xFF888888;
        }
        return r;
    }
}
