package org.test.byteinspector;

import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.orchestration.StatsManager;
import org.test.byteinspector.transformer.ClazzStatisticsTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Created by serkan on 30.12.2014.
 */
public class MethodAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) throws InterruptedException {
        StatisticsRepository instance = StatisticsRepository.INSTANCE;
        instrumentation.addTransformer(new ClazzStatisticsTransformer());
        StatsManager manager = new StatsManager();
        Thread.currentThread().sleep(1000);
        manager.start();
    }

}
