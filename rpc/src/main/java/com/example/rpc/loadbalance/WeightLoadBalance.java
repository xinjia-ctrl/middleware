package com.example.rpc.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightLoadBalance implements LoadBalance {

    private static final int DEFAULT_WEIGHT = 1;
    private final int[] weights;

    public WeightLoadBalance(int[] weights) {
        this.weights = weights;
    }

    @Override
    public String select(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("no available address found");
        }

        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        if (totalWeight <= 0) {
            return addresses.get(ThreadLocalRandom.current().nextInt(addresses.size()));
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        for (int i = 0; i < addresses.size(); i++) {
            int w = i < weights.length ? weights[i] : DEFAULT_WEIGHT;
            random -= w;
            if (random < 0) {
                return addresses.get(i);
            }
        }
        return addresses.get(addresses.size() - 1);
    }
}
