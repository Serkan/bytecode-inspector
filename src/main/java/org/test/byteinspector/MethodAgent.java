package org.test.byteinspector;

import org.test.byteinspector.model.MethodStatistics;
import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.transformer.ClazzStatisticsTransformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * Created by serkan on 30.12.2014.
 */
public class MethodAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClazzStatisticsTransformer());

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, MethodStatistics> stats = StatisticsRepository.INSTANCE.getStats();
                for (Map.Entry<String, MethodStatistics> statisticsEntry : stats.entrySet()) {
                    System.out.println(statisticsEntry.getKey() + " ==> " + statisticsEntry.getValue());
                }
                try {
                    Thread.currentThread().sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.setName("Monitoring Thread");
        System.out.println("Statistic Monitoring Started");
        t.start();
    }


}
