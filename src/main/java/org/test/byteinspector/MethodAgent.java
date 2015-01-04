package org.test.byteinspector;

import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.orchestration.StatsManager;
import org.test.byteinspector.transformer.ClazzStatisticsTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Instrumentation agent entry point.
 * <p/>
 * Usage :
 * <p/>
 * <code>
 * -Xbootclasspath/p:<bytecode-inspector>.jar
 * -javaagent:<bytecode-inspector>.jar
 * </code>
 * <p/>
 * both parameter must be specified when running
 * the jvm; <code>-Xbootclasspath</code> parameter tells the
 * bootstrap class loader recognize the bytcode
 * inspector classes otherwise {@link java.lang.NoClassDefFoundError} occurs,
 * <code>-javaagent</code> parametes specifies this is a
 * instrumentation agent look for agent entry point "premain" method.
 *
 * @author serkan
 */
public class MethodAgent {

    /**
     * Main method for agent, responsible to start statistics engine and monitoring threads.
     *
     * @param agentArgs       agent arguments
     * @param instrumentation instrumentation instance
     * @throws InterruptedException
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws InterruptedException {
        StatisticsRepository instance = StatisticsRepository.INSTANCE;
        instrumentation.addTransformer(new ClazzStatisticsTransformer());
        StatsManager manager = new StatsManager();
        Thread.currentThread().sleep(1000);
        manager.start();
    }

}
