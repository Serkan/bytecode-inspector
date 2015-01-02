package org.test.byteinspector.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by serkan on 30.12.2014.
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

    public void add(MethodStatistics other) {
        Set<Map.Entry<String, Double>> entries = other.entrySet();
        for (Map.Entry<String, Double> entry : entries) {
            String attributeKey = entry.getKey();
            Double thisVal = this.get(attributeKey);
            Double otherVal = entry.getValue();
            this.put(attributeKey, thisVal + otherVal);
        }
    }

    public void invokeEvent() {
        Double invokeCount = this.get("invokeCount");
        invokeCount++;
        this.put("invokeCount", invokeCount);
    }

}
