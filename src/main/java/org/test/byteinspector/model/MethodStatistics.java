package org.test.byteinspector.model;

import java.util.HashMap;

/**
 * Created by serkan on 30.12.2014.
 */
public class MethodStatistics extends HashMap<String, Double> {

    private String methodName;

    private boolean isComplete = false;

    public MethodStatistics(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }
}
