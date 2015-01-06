package org.test.byteinspector.orchestration;

import org.test.byteinspector.model.MethodDefContainer;
import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.repository.StatsTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is the method event's queue consumer thread.
 * It listens consumes queue by taking an event and
 * run it as {@link org.test.byteinspector.repository.StatsTask}
 * whenever an event available in the queue.
 *
 * @author serkan
 */
public class StatsManager extends Thread {

    private StatisticsRepository statisticsRepository;

    private int stop = 0;

    public StatsManager() {
        //TODO must be daeman in prod environment
//        this.setDaemon(true);
        this.setName("Stats Manager Thread");
        statisticsRepository = StatisticsRepository.INSTANCE;
    }

    @Override
    public void run() {
        BlockingQueue<MethodDefContainer> queue = statisticsRepository.queue();
        while (stop++ < 100000) {
            try {
                MethodDefContainer methodDefContainer = null;
                try {
                    methodDefContainer = queue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                StatsTask task = new StatsTask(methodDefContainer.getClazzName(), methodDefContainer.getMethodName());
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Analysis started");
        AnalyzerManager analyzer = new AnalyzerManager();
        analyzer.start();
    }
}
