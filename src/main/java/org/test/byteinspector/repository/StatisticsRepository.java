package org.test.byteinspector.repository;

import org.test.byteinspector.model.MethodDefContainer;
import org.test.byteinspector.model.MethodStatistics;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The memory to hold method statistics and method event queue.
 * Implemented as enum singleton for thread safety.
 *
 * @author serkan
 */
public enum StatisticsRepository {
    INSTANCE;

    // method invocation event queue
    private BlockingQueue<MethodDefContainer> queue;

    // statistics memory
    private ConcurrentMap<String, MethodStatistics> statsMap;

    /**
     * Default constructor to init. queue and hash map.
     */
    StatisticsRepository() {
        queue = new LinkedBlockingQueue<>();
        statsMap = new ConcurrentHashMap<>();
    }

    /**
     * Records the specified method statistics in memory.
     *
     * @param stats method statistics
     */
    public void put(MethodStatistics stats) {
        String methodName = stats.getMethodName();
        statsMap.put(methodName, stats);
    }

    /**
     * Getter for method statistics.
     *
     * @param methodName method name
     * @return null if nothing matches with specified method name
     */
    public MethodStatistics get(String methodName) {
        return statsMap.get(methodName);
    }

    public void invokeEvent(String clazzName, String methodName) {
//        System.out.println("INVOKED EVENT : " + clazzName + "->" + methodName);
        queue.add(new MethodDefContainer(clazzName, methodName));
    }

    /**
     * Getter for statistics memory.
     *
     * @return
     */
    public Map<String, MethodStatistics> getStats() {
        return statsMap;
    }

    /**
     * Getter for method invocation event queue.
     *
     * @return blocking queue used to block consumer
     * threads if there is nothing to consume.
     */
    public BlockingQueue<MethodDefContainer> queue() {
        return queue;
    }

}
