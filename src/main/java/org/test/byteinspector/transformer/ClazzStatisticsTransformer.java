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
 * Created by serkan on 30.12.2014.
 */
public class ClazzStatisticsTransformer implements ClassFileTransformer {

    public ClazzStatisticsTransformer() {
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode;
//        if (className.startsWith("com/intellij")) {
//            return classfileBuffer;
//        }
        try {
            ClassPool cp = ClassPool.getDefault();
            cp.appendClassPath(new LoaderClassPath(loader));
            cp.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));
            String clazz = className.replace("/", ".");
            CtClass cc = cp.get(clazz);
            CtMethod[] methods = cc.getMethods();
            for (CtMethod method : methods) {
                if (!Modifier.isNative(method.getModifiers()) &&
                        !method.getLongName().contains("Launcher") &&
                        !method.getLongName().contains("StatisticsRepository")) {
                    StringBuilder buffer = new StringBuilder();
                    String name = method.getName();
                    buffer.append(name + "|");
                    CtClass[] parameterTypes = method.getParameterTypes();
                    for (CtClass parameterType : parameterTypes) {
                        String paramName = parameterType.getName();
                        buffer.append(paramName);
                        buffer.append(",");
                    }
                    String methodSig = buffer.toString();
                    methodSig = methodSig.substring(0, methodSig.length() - 1);
                    String insertCode = "org.test.byteinspector.repository.StatisticsRepository.INSTANCE.invokeEvent(\"" + clazz + "\",\"" + methodSig + "\");";
                    method.insertBefore(insertCode);
                }
            }
            byteCode = cc.toBytecode();
            cc.detach();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return byteCode;
    }

}
