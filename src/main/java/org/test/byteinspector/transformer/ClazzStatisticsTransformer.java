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

    private Set<String> visited = new HashSet<>();

    public ClazzStatisticsTransformer() {
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode;
        try {
            ClassPool cp = ClassPool.getDefault();
            cp.appendClassPath(new LoaderClassPath(loader));
            cp.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));
            CtClass cc = cp.get(className.replace("/", "."));
            CtMethod[] methods = cc.getMethods();
            for (CtMethod method : methods) {
                if (!Modifier.isNative(method.getModifiers()) &&
                        !visited.contains(method.getLongName()) &&
                        !method.getLongName().contains("Launcher") &&
                        !method.getLongName().contains("StatisticsRepository")) {
                    String insertCode = "org.test.byteinspector.repository.StatisticsRepository.INSTANCE.invokeEvent(\"" + method.getLongName() + "\");";
                    method.insertBefore(insertCode);
                    visited.add(method.getLongName());
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
