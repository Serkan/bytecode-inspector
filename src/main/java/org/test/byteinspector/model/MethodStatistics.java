package org.test.byteinspector.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Data structure to hold method statistics. It is subclass
 * of {@link java.util.HashMap} because instead of staticly
 * keep statistic data, this structure let client classes
 * dynamic implementations which can adapt change of attributes.
 *
 * @author serkan
 */
public class MethodStatistics extends HashMap<String, Double> {

    private CalculationState state;

    private String methodName;

    private int clusterId;

    public MethodStatistics(String methodName) {
        this.methodName = methodName;
        this.state = CalculationState.CALCULATING;
    }


    public String getMethodName() {
        return methodName;
    }

    public CalculationState getState() {
        return state;
    }

    public void setState(CalculationState state) {
        this.state = state;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * Arithmetic add operation. Important thing
     * is this method does not return a new instance
     * instead modifies the object itself but do not
     * affect given method statistics.
     *
     * @param other right side of add operation
     */
    public void add(MethodStatistics other) {
        Set<Map.Entry<String, Double>> entries = other.entrySet();
        for (Map.Entry<String, Double> entry : entries) {
            String attributeKey = entry.getKey();
            Double thisVal = this.get(attributeKey);
            Double otherVal = entry.getValue();
            this.put(attributeKey, thisVal + otherVal);
        }
    }

    /**
     * Invocation event is responsible to alter
     * some attributeswhen the event occurs.
     */
    public void invokeEvent() {
        Double invokeCount = this.get("invokeCount");
        invokeCount++;
        this.put("invokeCount", invokeCount);
    }

}
