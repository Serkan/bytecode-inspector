package org.test.byteinspector.repository;

import org.test.byteinspector.model.MethodStatistics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by serkan on 30.12.2014.
 */
public enum StatisticsRepository {
    INSTANCE;

    private Map<String, MethodStatistics> statsMap = new HashMap<>();

    public void put(MethodStatistics stats) {
        String methodName = stats.getMethodName();
        statsMap.put(methodName, stats);
    }

    public MethodStatistics get(String methodName) {
        return statsMap.get(methodName);
    }

    public void invokeEvent(String methodName) {
        System.out.println("############INVOKE EVENT###########");
        MethodStatistics m = statsMap.get(methodName);
        if (m != null) {
            synchronized (m) {
                Double invokeCount = m.get("invokeCount");
                invokeCount++;
                m.put("invokeCount", invokeCount);
            }
        }
    }

    public Map<String, MethodStatistics> getStats() {
        return statsMap;
    }

}
