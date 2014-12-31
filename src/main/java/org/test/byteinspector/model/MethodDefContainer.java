package org.test.byteinspector.model;

/**
 * Created by serkan on 31.12.2014.
 */
public class MethodDefContainer {

    private String clazzName;

    private String methodName;

    public MethodDefContainer(String clazzName, String methodName) {
        this.clazzName = clazzName;
        this.methodName = methodName;
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
