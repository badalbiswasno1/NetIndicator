package com.my.netindicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatisticsEngine {

    public static class Result {
        public long minPing = -1;
        public long maxPing = -1;
        public long avgPing = -1;
        public long medianPing = -1;
        public int bestSignal = 0;
        public int worstSignal = 0;
        public double avgGrade = 0;
        public String bestGradeStr = "--";
        public String worstGradeStr = "--";
        public long totalDataKB = 0;
        public int recordCount = 0;
    }

    public static Result compute(JSONArray logs, long cutoffTimeMillis) {
        Result r = new Result();
        try {
            List<Long> pings = new ArrayList<>();
            List<Integer> signals = new ArrayList<>();
            List<Double> grades = new ArrayList<>();
            long maxDataKB = 0;
            String bestGrade = null;
            String worstGrade = null;
            double bestGradeVal = -1;
            double worstGradeVal = 999;

            for (int i = 0; i < logs.length(); i++) {
                JSONObject obj = logs.getJSONObject(i);
                long timestamp = obj.optLong("timestamp", 0);
                if (cutoffTimeMillis > 0 && timestamp < cutoffTimeMillis) continue;

                long ping = obj.getLong("ping");
                if (ping >= 0) pings.add(ping);

                int signal = obj.optInt("signal", 0);
                if (signal != 0) signals.add(signal);

                String gradeStr = obj.getString("grade");
                double gradeVal = parseGrade(gradeStr);
                if (gradeVal > 0) {
                    grades.add(gradeVal);
                    if (gradeVal > bestGradeVal) {
                        bestGradeVal = gradeVal;
                        bestGrade = gradeStr;
                    }
                    if (gradeVal < worstGradeVal) {
                        worstGradeVal = gradeVal;
                        worstGrade = gradeStr;
                    }
                }

                long dataKB = obj.optLong("data", 0);
                if (dataKB > maxDataKB) maxDataKB = dataKB;

                r.recordCount++;
            }

            if (!pings.isEmpty()) {
                Collections.sort(pings);
                r.minPing = pings.get(0);
                r.maxPing = pings.get(pings.size() - 1);
                long sum = 0;
                for (long p : pings) sum += p;
                r.avgPing = sum / pings.size();
                r.medianPing = pings.get(pings.size() / 2);
            }

            if (!signals.isEmpty()) {
                Collections.sort(signals);
                r.worstSignal = signals.get(0);
                r.bestSignal = signals.get(signals.size() - 1);
            }

            if (!grades.isEmpty()) {
                double sum = 0;
                for (double g : grades) sum += g;
                r.avgGrade = sum / grades.size();
            }

            r.bestGradeStr = bestGrade != null ? bestGrade : "--";
            r.worstGradeStr = worstGrade != null ? worstGrade : "--";
            r.totalDataKB = maxDataKB;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    private static double parseGrade(String grade) {
        try {
            return Double.parseDouble(grade.replace("G", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
