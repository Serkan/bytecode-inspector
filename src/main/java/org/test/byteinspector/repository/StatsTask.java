package org.test.byteinspector.repository;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.test.byteinspector.model.MethodStatistics;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Created by serkan on 31.12.2014.
 */
public class StatsTask implements Runnable {

    private String clazzName;

    private String methodName;

    public StatsTask(String clazzName, String methodName) {
        this.clazzName = clazzName;
        this.methodName = methodName;
    }

    @Override
    public void run() {
        //TODO check method evaluated before or first time gets here
        // TODO then either increase invokeCount or calculate method statistics
//        Method reflectedMethod = getReflectedMethod(clazzName, methodName);
//        MethodStatistics methodStatistics = calculate(reflectedMethod);
//        MethodStatistics m = statsMap.get(methodName);
//        if (m == null) {
//            m = new MethodStatistics(methodName);
//            m.put("invokeCount", 0d);
//            statsMap.put(methodName, m);
//        }
//        synchronized (m) {
//            Double invokeCount = m.get("invokeCount");
//            invokeCount++;
//            m.put("invokeCount", invokeCount);
//        }
    }

    private MethodStatistics calculate(Method method) {
        ClassParser parser;
        JavaClass clazz;
        try {
            parser = new ClassParser(clazzName);
            clazz = parser.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        org.apache.bcel.classfile.Method bcelMethod = clazz.getMethod(method);
        int methodLength = bcelMethod.getCode().getLength();
        ConstantPoolGen cpg = new ConstantPoolGen(clazz.getConstantPool());
        MethodGen mg = new MethodGen(bcelMethod, clazz.getClassName(), cpg);
        InstructionList instructionList = mg.getInstructionList();
        Iterator insIt = instructionList.iterator();
        int localVariableTableSize = mg.getLocalVariables().length;
        int invokeCount = 0;
        int newOpCount = 0;
        int throwOpCount = 0;
        int directIOCount = 0;
        int fieldAccessCount = 0;
        int branchCount = 0;
        int loopCount = 0;
        while (insIt.hasNext()) {
            InstructionHandle insHandle = (InstructionHandle) insIt.next();
            Instruction instruction = insHandle.getInstruction();
            if (instruction instanceof InvokeInstruction) {
                invokeCount++;
                InvokeInstruction ii = (InvokeInstruction) instruction;
                ReferenceType referenceType = ii.getReferenceType(cpg);
                String callee = ii.getName(cpg);
                String calleeFullName = referenceType + "." + callee;
                if (calleeFullName.equals("java.io.FileInputStream.read") ||
                        calleeFullName.equals("java.io.OutputStream.write") ||
                        calleeFullName.equals("java.net.SocketInputStream.read") ||
                        calleeFullName.equals("java.net.SocketOutputStream.write")) {
                    directIOCount++;
                }
            } else if (instruction instanceof NEW) {
                newOpCount++;
            } else if (instruction instanceof ATHROW) {
                throwOpCount++;
            } else if (instruction instanceof FieldInstruction) {
                fieldAccessCount++;
            } else if (instruction instanceof BranchInstruction) {
                // if this is a backward jump identify it as loop otherwise its just a branch
                BranchInstruction bi = (BranchInstruction) instruction;
                int targetPos = bi.getTarget().getPosition();
                int currentPos = insHandle.getPosition();
                if (targetPos < currentPos) {
                    loopCount++;
                } else {
                    branchCount++;
                }
            }
        }
        MethodStatistics stats = new MethodStatistics(methodName);
        stats.put("localVariableTableSize", Double.valueOf(localVariableTableSize));
        stats.put("invokeCount", Double.valueOf(invokeCount));
        stats.put("newOpCount", Double.valueOf(newOpCount));
        stats.put("throwOpCount", Double.valueOf(throwOpCount));
        stats.put("directIOCount", Double.valueOf(directIOCount));
        stats.put("fieldAccessCount", Double.valueOf(fieldAccessCount));
        stats.put("branchCount", Double.valueOf(branchCount));
        stats.put("loopCount", Double.valueOf(loopCount));
        return stats;
    }

    private Method getReflectedMethod(String clazzName, String methodName) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(clazzName);
        Method method;
        if (methodName.contains("|")) {
            String[] signature = methodName.split("\\|");
            String paramStr = signature[1];
            String[] paramTypes = paramStr.split(",");
            Class<?>[] paramClazzArray = new Class<?>[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramClazz = Class.forName(paramTypes[i]);
                paramClazzArray[i] = paramClazz;
            }
            method = clazz.getMethod(signature[0], paramClazzArray);
        } else {
            method = clazz.getMethod(methodName);
        }
        return method;
    }
}
