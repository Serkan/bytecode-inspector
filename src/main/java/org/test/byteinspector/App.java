package org.test.byteinspector;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.test.byteinspector.repository.StatisticsRepository;

import java.io.IOException;
import java.util.Iterator;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException {
        ClassParser parser = new ClassParser("/home/serkan/dev/scala/testbed/target/classes/org/test/App.class");
        JavaClass clazz = parser.parse();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            visitMethod(clazz, method);
        }
    }

    private static void visitMethod(JavaClass clazz, Method method) {
        int methodLength = method.getCode().getLength();
        ConstantPoolGen cpg = new ConstantPoolGen(clazz.getConstantPool());
        MethodGen mg = new MethodGen(method, clazz.getClassName(), cpg);
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
        System.out.println("Invoke count : " + invokeCount);
        System.out.println("new operator count : " + newOpCount);
        System.out.println("Throw op count: " + throwOpCount);
        System.out.println("Direct IO Count: " + directIOCount);
        System.out.println("Method Length: " + methodLength);
        System.out.println("Field access count : " + fieldAccessCount);
        System.out.println("Branch count : " + branchCount);
        System.out.println("Loop count : " + loopCount);
        System.out.println("Local variable table size: " + localVariableTableSize);
    }
}
