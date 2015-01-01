package org.test.byteinspector.orchestration;

import org.test.byteinspector.model.MethodStatistics;
import org.test.byteinspector.repository.StatisticsRepository;

import java.util.*;

/**
 * Created by serkan on 01.01.2015.
 */
public class AnalyzerManager extends Thread {

    private Map<String, Double> sum = new HashMap<>();

    @Override
    public void run() {
        normalize();
        cluster();
    }

    private void cluster() {
        Map<String, MethodStatistics> stats = StatisticsRepository.INSTANCE.getStats();
        // initial cluster count
        /*
        number of clusters picked by the rule of thumb, because here important thing is
        not to collect all similar data points one cluster, to find similar candidates
        for JIT compilation
         */
        int n = stats.size();
        int clusterCount = (int) Math.round(Math.sqrt(n / 2));

        // pick random centroids
        MethodStatistics methodStatistics = (MethodStatistics) stats.entrySet().toArray()[0];
        Set<String> attributeList = methodStatistics.keySet();
        List<MethodStatistics> centroids = randomK(clusterCount, attributeList);
        List<MethodStatistics> reCalculatedCentroids = null;

        // key is the clusterId, value is the count of data points in that cluster
        Map<Integer, Integer> clusterSizes;
        // run k-means algorithm until its converged
        do {
            // if this isn't the first pass, change centroids with recently calculated ones
            if (reCalculatedCentroids != null) {
                centroids = reCalculatedCentroids;
            }
            Collection<MethodStatistics> values = stats.values();
            // initialize sum hash table to calculate new centroids
            // key is the clusterId, value is the method statistics which attribute values are totals intra-cluster
            Map<Integer, MethodStatistics> totals = new HashMap<>(clusterCount);
            clusterSizes = new HashMap<>(clusterCount);
            for (int i = 0; i < clusterCount; i++) {
                MethodStatistics total = new MethodStatistics("TOTAL" + i);
                for (String attr : attributeList) {
                    total.put(attr, 0d);
                }
                totals.put(i, total);
                clusterSizes.put(i, 0);
            }
            // assign data points to clusters
            for (MethodStatistics statistics : values) {
                // find which is the closest centroid
                assignToClosestCentroid(statistics, centroids);
                // collect aggregation data for further calculations (sum and count)
                int newClusterId = statistics.getClusterId();
                MethodStatistics attributeTotals = totals.get(newClusterId);
                // add current data point's attribute values to cluster's total
                attributeTotals.add(statistics);
                // increase cluster size by 1
                Integer clusterSize = clusterSizes.get(newClusterId);
                clusterSizes.put(newClusterId, ++clusterSize);
            }
            reCalculatedCentroids = new LinkedList<>();
            for (Map.Entry<Integer, MethodStatistics> totalEntry : totals.entrySet()) {
                Integer clusterId = totalEntry.getKey();
                MethodStatistics clusterTotal = totalEntry.getValue();
                Integer clusterSize = clusterSizes.get(clusterId);
                MethodStatistics newCentroid = new MethodStatistics("DUMMY");
                for (Map.Entry<String, Double> attributeEntry : clusterTotal.entrySet()) {
                    String attrName = attributeEntry.getKey();
                    Double attrVal = attributeEntry.getValue();
                    newCentroid.put(attrName, attrVal / clusterSize);
                }
                reCalculatedCentroids.add(newCentroid);
            }
        } while (!centroids.equals(reCalculatedCentroids));
        // find candidate clusters to send JIT

    }

    private void assignToClosestCentroid(MethodStatistics statistics, List<MethodStatistics> centroids) {
        double min = Double.MAX_VALUE;
        for (MethodStatistics centroid : centroids) {
            double distance = distance(statistics, centroid);
            if (distance < min) {
                // assign the data point to centroid's cluster
                statistics.setClusterId(centroid.getClusterId());
                //change the minimum
                min = distance;
            }
        }
    }

    private List<MethodStatistics> randomK(int centroidCount, Set<String> attributeList) {
        List<MethodStatistics> centroids = new LinkedList<>();
        Random rnd = new Random();
        for (int i = 0; i < centroidCount; i++) {
            MethodStatistics randomCent = new MethodStatistics("DUMMY");
            for (String attribute : attributeList) {
                randomCent.put(attribute, rnd.nextDouble());
            }
            randomCent.setClusterId(i);
            centroids.add(randomCent);
        }
        return centroids;
    }

    private double distance(MethodStatistics m1, MethodStatistics m2) {
        // Euclidean distance
        double total = 0;
        for (Map.Entry<String, Double> attr1 : m1.entrySet()) {
            String attrName = attr1.getKey();
            Double v1 = attr1.getValue();
            Double v2 = m2.get(attrName);
            total += Math.pow(v1 - v2, 2);
        }
        return Math.sqrt(total);
    }

    private void normalize() {
        Map<String, MethodStatistics> stats = StatisticsRepository.INSTANCE.getStats();
        for (Map.Entry<String, MethodStatistics> entry : stats.entrySet()) {
            MethodStatistics methodStats = entry.getValue();
            for (Map.Entry<String, Double> methodStat : methodStats.entrySet()) {
                String statName = methodStat.getKey();
                Double statValue = methodStat.getValue();
                Double statSum = sum.get(statName);
                if (statSum == null) {
                    sum.put(statName, new Double(statValue));
                } else {
                    sum.put(statName, statSum + statValue);
                }
            }
        }
        for (Map.Entry<String, MethodStatistics> entry : stats.entrySet()) {
            MethodStatistics methodStats = entry.getValue();
            for (Map.Entry<String, Double> methodStat : methodStats.entrySet()) {
                String statName = methodStat.getKey();
                Double statValue = methodStat.getValue();
                Double statSum = sum.get(statName);
                if (statSum == 0) {
                    continue;
                }
                methodStats.put(statName, statValue / statSum);
            }
        }
    }
}
