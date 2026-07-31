package com.my.netindicator;

public class NetworkHealthEngine {

    public static class Result {
        public long avgPing;
        public long jitter;
        public int packetLossPercent;
        public long dnsMs;
        public int healthPercent;
        public String dnsLabel;
        public String latencyLabel;
    }

    public static Result measure() {
        Result r = new Result();
        int samples = 5;
        long[] pings = new long[samples];
        int timeouts = 0;

        for (int i = 0; i < samples; i++) {
            long p = singlePing();
            pings[i] = p;
            if (p < 0) timeouts++;
        }

        long sum = 0;
        int validCount = 0;
        for (long p : pings) {
            if (p >= 0) {
                sum += p;
                validCount++;
            }
        }
        r.avgPing = validCount > 0 ? sum / validCount : -1;
        r.packetLossPercent = (timeouts * 100) / samples;

        long jitterSum = 0;
        int jitterCount = 0;
        for (int i = 1; i < samples; i++) {
            if (pings[i] >= 0 && pings[i - 1] >= 0) {
                jitterSum += Math.abs(pings[i] - pings[i - 1]);
                jitterCount++;
            }
        }
        r.jitter = jitterCount > 0 ? jitterSum / jitterCount : 0;

        r.dnsMs = measureDns();

        double latencyScore = r.avgPing < 0 ? 0 : Math.max(0, Math.min(100, 100 - (r.avgPing / 3.0)));
        double lossScore = 100 - (r.packetLossPercent * 2.0);
        double jitterScore = Math.max(0, 100 - (r.jitter * 2.0));
        double dnsScore = r.dnsMs < 0 ? 50 : Math.max(0, Math.min(100, 100 - (r.dnsMs / 5.0)));

        double overall = latencyScore * 0.35 + lossScore * 0.30 + jitterScore * 0.20 + dnsScore * 0.15;
        r.healthPercent = (int) Math.max(0, Math.min(100, Math.round(overall)));

        r.dnsLabel = r.dnsMs < 0 ? "Failed" : r.dnsMs < 50 ? "Excellent" : r.dnsMs < 150 ? "Good" : "Slow";
        r.latencyLabel = r.avgPing < 0 ? "Failed" : r.avgPing < 50 ? "Excellent" : r.avgPing < 150 ? "Good" : "Poor";

        return r;
    }

    private static long singlePing() {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 2 8.8.8.8");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("time=")) {
                    int start = line.indexOf("time=") + 5;
                    int end = line.indexOf(" ms", start);
                    return (long) Float.parseFloat(line.substring(start, end));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static long measureDns() {
        try {
            long start = System.currentTimeMillis();
            java.net.InetAddress.getByName("google.com");
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }
}
