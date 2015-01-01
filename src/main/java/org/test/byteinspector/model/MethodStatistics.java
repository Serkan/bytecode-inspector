package org.test.byteinspector.model;

import java.util.HashMap;

/**
 * Created by serkan on 30.12.2014.
 */
public class MethodStatistics extends HashMap<String, Double> {

    private CalculationState state;

    private String methodName;

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

    public void invokeEvent() {
        Double invokeCount = this.get("invokeCount");
        invokeCount++;
        this.put("invokeCount", invokeCount);
    }

}
