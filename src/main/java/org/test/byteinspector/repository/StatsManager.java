package org.test.byteinspector.repository;

import org.test.byteinspector.model.MethodDefContainer;

import java.util.concurrent.BlockingQueue;

/**
 * Created by serkan on 31.12.2014.
 */
public class StatsManager extends Thread {

    private StatisticsRepository statisticsRepository;

    public StatsManager() {
//        this.setDaemon(true);
        this.setName("Stats Manager Thread");
        statisticsRepository = StatisticsRepository.INSTANCE;
    }

    @Override
    public void run() {
        BlockingQueue<MethodDefContainer> queue = statisticsRepository.queue();
        while (true) {
            MethodDefContainer methodDefContainer = null;
            try {
                methodDefContainer = queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            StatsTask task = new StatsTask(methodDefContainer.getClazzName(), methodDefContainer.getMethodName());
            task.run();
        }
    }
}
