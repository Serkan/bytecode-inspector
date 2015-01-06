package org.test.byteinspector.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * Class file to transformer, injects monitoring code to method body.
 *
 * @author serkan
 */
public class ClazzStatisticsTransformer implements ClassFileTransformer {

    /**
     * @see java.lang.instrument.ClassFileTransformer
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode;
        // test environment check and make sure u dont change agent code otherwise agent enters infinite loop
        if (className.startsWith("org/test") ||
                className.startsWith("javassist/") ||
                className.startsWith("sun/instrument") ||
                className.startsWith("org/apache/bcel") /*||
                className.startsWith("java/util/concurrent/locks/LockSupport/park") */) {
            return classfileBuffer;
        }
        try {
            ClassPool cp = new ClassPool(true);
            // javasist needs to know where to find reflected class
            cp.appendClassPath(new LoaderClassPath(loader));
            cp.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));
            // tiny correction from file path to class name
            CtMethod[] methods;
            String clazz = className.replace("/", ".");
            CtClass cc = cp.get(clazz);
            methods = cc.getMethods();
            for (CtMethod method : methods) {
                // we do not want interfere native methods because they already compiled
                // or change logic of sun launcher classes, otherwise application can not start
                if (!Modifier.isNative(method.getModifiers()) &&
                        !method.getLongName().contains("Launcher") &&
                        !method.getLongName().contains("StatisticsRepository")) {
                    // build a string that contains class name, method name and method signature
                    StringBuilder buffer = new StringBuilder();
                    String name = method.getName();
                    buffer.append(name + "|");
                    CtClass[] parameterTypes = method.getParameterTypes();
                    for (CtClass parameterType : parameterTypes) {
                        String paramName = parameterType.getName();
                        paramName = paramName.replace("/", ".");
                        buffer.append(paramName);
                        buffer.append(",");
                    }
                    String methodSig = buffer.toString();
                    // clear trailing comma
                    methodSig = methodSig.substring(0, methodSig.length() - 1);
                    // inject monitoring code method beginning point
                    String insertCode = "org.test.byteinspector.repository.StatisticsRepository.INSTANCE.invokeEvent(\"" + clazz + "\",\"" + methodSig + "\");";
                    method.insertBefore(insertCode);
                }
            }
            byteCode = cc.toBytecode();
            cc.detach();
        } catch (Exception ex) {
            // even we throw a runtime exception in case of error,
            // Instrumentation manager do not let agent thread failed completely,
            // ignores any exception and move on silently (without printing any message)
            throw new RuntimeException(ex);
        }
        return byteCode;
    }

}
