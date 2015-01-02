package org.test.byteinspector.orchestration;

import org.test.byteinspector.model.MethodDefContainer;
import org.test.byteinspector.repository.StatisticsRepository;
import org.test.byteinspector.repository.StatsTask;

import java.util.concurrent.BlockingQueue;

/**
 * Created by serkan on 31.12.2014.
 */
public class StatsManager extends Thread {

    private StatisticsRepository statisticsRepository;

    private int stop = 0;

    public StatsManager() {
//        this.setDaemon(true);
        this.setName("Stats Manager Thread");
        statisticsRepository = StatisticsRepository.INSTANCE;
    }

    @Override
    public void run() {
        BlockingQueue<MethodDefContainer> queue = statisticsRepository.queue();
        while (stop++ < 100000) {
            MethodDefContainer methodDefContainer = null;
            try {
                methodDefContainer = queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            StatsTask task = new StatsTask(methodDefContainer.getClazzName(), methodDefContainer.getMethodName());
            task.run();
        }
        AnalyzerManager analyzer = new AnalyzerManager();
        analyzer.start();
    }
}
