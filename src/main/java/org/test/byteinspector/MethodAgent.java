package org.test.byteinspector;

import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.orchestration.StatsManager;
import org.test.byteinspector.transformer.ClazzStatisticsTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Instrumentation agent entry point.
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
