package org.test.byteinspector.repository;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.apache.commons.lang.StringUtils;
import org.test.byteinspector.model.CalculationException;
import org.test.byteinspector.model.ClassFileLocation;
import org.test.byteinspector.model.MethodStatistics;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * It is responsible for extracting features from a class method.
 * If a method structure analyzed before it just calls invoke event
 * to change live statistics belongs to that method. Implemented as a
 * {@link java.lang.Runnable} task thus it can be used multi thread
 * environment.
 *
 * @author serkan
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
        MethodStatistics methodStatistics = StatisticsRepository.INSTANCE.get(methodName);
        if (methodStatistics == null) {
            Method reflectedMethod = null;
            try {
                reflectedMethod = getReflectedMethod(clazzName, methodName);
                methodStatistics = calculate(reflectedMethod);
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchElementException e) {
                throw new RuntimeException(e);
            } catch (CalculationException e) {
                // no need to proceed further
                return;
            }
            methodStatistics.invokeEvent();
            StatisticsRepository.INSTANCE.put(methodStatistics);
        } else {
            methodStatistics.invokeEvent();
        }
    }

    /**
     * It produces method statistics for specified method instance.
     * Apache bytecode engineering library heavily used to obtain
     * bytecode information. Counts the bytecode to calculate statistics.
     *
     * @param method method instance
     * @return method stats
     * @throws CalculationException if bytecode can not obtained somehow
     */
    private MethodStatistics calculate(Method method) throws CalculationException {
        ClassFileLocation location = null;
        ClassParser parser;
        JavaClass clazz;
        try {
            location = getLocation(clazzName);
            if (location.isZip()) {
                parser = new ClassParser(location.getZipName(), location.getFileName());
            } else {
                parser = new ClassParser(location.getFileName());
            }
            clazz = parser.parse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        org.apache.bcel.classfile.Method bcelMethod = clazz.getMethod(method);
        if (bcelMethod == null) {
            throw new CalculationException("Bcel method can not be obtained");
        }
        Code methodCode = bcelMethod.getCode();
        int methodLength = methodCode.getLength();
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
        stats.put("methodLength", Double.valueOf(methodLength));
        return stats;
    }

    /**
     * Finds the {@link java.lang.reflect.Method} object for
     * specified class name and method name. Method name must
     * be a specific format for appropriate result. For instance,
     * for replace method of {@link java.lang.String} method name
     * formatted as "replace|char,char" to represent the full signature.
     *
     * @param clazzName  class name
     * @param methodName method name formatted as described
     * @return reflected {@link java.lang.reflect.Method} method instance
     * @throws ClassNotFoundException if can not find the reflected class
     * @throws NoSuchMethodException  if can not find reflected method
     */
    private Method getReflectedMethod(String clazzName, String methodName) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(clazzName);
        Method method;
        if (methodName.contains("|")) {
            String[] signature = methodName.split("\\|");
            String paramStr = signature[1];
            String[] paramTypes = paramStr.split(",");
            Class<?>[] paramClazzArray = new Class<?>[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                String paramClazzName = paramTypes[i];
                Class<?> paramClazz = null;
                // "[Ljava.lang.String;"
                // if parameters is an array type change it appropriate string
                if (paramClazzName.endsWith("[]")) {
                    // for primitive array type specific representations must be used
                    if (paramClazzName.startsWith("int")) {
                        paramClazzName = "[I";
                    } else if (paramClazzName.startsWith("float")) {
                        paramClazzName = "[F";
                    } else if (paramClazzName.startsWith("double")) {
                        paramClazzName = "[D";
                    } else if (paramClazzName.startsWith("char")) {
                        paramClazzName = "[C";
                    } else if (paramClazzName.startsWith("byte")) {
                        paramClazzName = "[B";
                    } else if (paramClazzName.startsWith("long")) {
                        paramClazzName = "[J";
                    } else if (paramClazzName.startsWith("short")) {
                        paramClazzName = "[S";
                    } else if (paramClazzName.startsWith("boolean")) {
                        paramClazzName = "[Z";
                    } else {
                        // for class arrays [L prefix and ; suffix is necessary format
                        paramClazzName = "[L" + paramClazzName.substring(0, paramClazzName.length() - 2) + ";";
                    }
                    // for primitive parameters type primitive class type
                    // must be obtained in runtime,
                    // boxed versions class name does not work
                } else if (paramClazzName.equals("int")) {
                    paramClazz = int.class;
                } else if (paramClazzName.equals("float")) {
                    paramClazz = float.class;
                } else if (paramClazzName.equals("double")) {
                    paramClazz = double.class;
                } else if (paramClazzName.equals("char")) {
                    paramClazz = char.class;
                } else if (paramClazzName.equals("byte")) {
                    paramClazz = byte.class;
                } else if (paramClazzName.equals("long")) {
                    paramClazz = long.class;
                } else if (paramClazzName.equals("short")) {
                    paramClazz = short.class;
                } else if (paramClazzName.equals("boolean")) {
                    paramClazz = boolean.class;
                }
                if (paramClazz == null) {
                    paramClazz = Class.forName(paramClazzName);
                }
                paramClazzArray[i] = paramClazz;
            }
            method = clazz.getDeclaredMethod(signature[0], paramClazzArray);
        } else {
            method = clazz.getDeclaredMethod(methodName);
        }
        return method;
    }

    /**
     * Locates the class file for given class name.
     * For instance for a string "java.lang.Integer" finds
     * the jar file and exact location in the jar file.
     *
     * @param clazzName class name
     * @return ClassFileLocation that contains where and how located .class file.
     */
    private ClassFileLocation getLocation(String clazzName) {
        ClassLoader loader = getClass().getClassLoader();
        String resourceName = clazzName.replace(".", "/") + ".class";
        // if there is no class loader, obtain the bootstrap class loader
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader().getParent();
        }
        URL resource = loader.getResource(resourceName);
        String url = resource.toString();
        //jar:file:/home/serkan/IdeaProjects/testproject/target/testproject-1.0-SNAPSHOT.jar!/org/test/App.class
        //file:/home/serkan/IdeaProjects/testproject/target/classes/org/test/App.class
        String prefix;
        ClassFileLocation location;
        if (url.startsWith("jar")) {
            prefix = "jar:file";
            url = url.replace(prefix, "");
            String[] parts = url.split("!");
            String zipLocation = parts[0];
            zipLocation = zipLocation.substring(1, zipLocation.length());
            String fileLocation = parts[1];
            fileLocation = fileLocation.substring(1, fileLocation.length());
            location = new ClassFileLocation();
            location.setZip(true);
            location.setZipName(zipLocation);
            location.setFileName(fileLocation);
        } else {
            // TODO
            throw new UnsupportedOperationException();
        }
        return location;
    }

}
