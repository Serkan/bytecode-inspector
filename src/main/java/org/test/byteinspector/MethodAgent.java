package org.test.byteinspector;

import org.test.byteinspector.model.MethodDefContainer;
import org.test.byteinspector.model.MethodStatistics;
import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.repository.StatsTask;
import org.test.byteinspector.transformer.ClazzStatisticsTransformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

/**
 * Created by serkan on 30.12.2014.
 */
public class MethodAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        StatisticsRepository instance = StatisticsRepository.INSTANCE;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                BlockingQueue<MethodDefContainer> queue = StatisticsRepository.INSTANCE.queue();
                while (true) {
                    MethodDefContainer methodDefContainer = null;
                    try {
                        methodDefContainer = queue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    StatsTask task = new StatsTask(methodDefContainer.getClazzName(), methodDefContainer.getMethodName());
                    task.run();
                }
            }
        });
        t.start();
        t.setDaemon(true);
        instrumentation.addTransformer(new ClazzStatisticsTransformer());

//        Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Map<String, MethodStatistics> stats = StatisticsRepository.INSTANCE.getStats();
//                try {
//                    Thread.currentThread().sleep(2000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        t.setName("Monitoring Thread");
//        System.out.println("Statistic Monitoring Started");
//        t.start();
    }


}
