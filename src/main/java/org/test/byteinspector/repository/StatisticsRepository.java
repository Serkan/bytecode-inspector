package org.test.byteinspector.repository;

import org.test.byteinspector.model.MethodDefContainer;
import org.test.byteinspector.model.MethodStatistics;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by serkan on 30.12.2014.
 */
public enum StatisticsRepository {
    INSTANCE;

    private BlockingQueue<MethodDefContainer> queue;

    private ConcurrentMap<String, MethodStatistics> statsMap;

    StatisticsRepository() {
        queue = new LinkedBlockingQueue<>();
        statsMap = new ConcurrentHashMap<>();
    }

    public void put(MethodStatistics stats) {
        String methodName = stats.getMethodName();
        statsMap.put(methodName, stats);
    }

    public MethodStatistics get(String methodName) {
        return statsMap.get(methodName);
    }

    public void invokeEvent(String clazzName, String methodName) {
        System.out.println("INVOKED CLASS : " + clazzName);
        System.out.println("INVOKED METHOD : " + methodName);
        queue.add(new MethodDefContainer(clazzName, methodName));
    }

    public Map<String, MethodStatistics> getStats() {
        return statsMap;
    }

    public BlockingQueue<MethodDefContainer> queue() {
        return queue;
    }

}
