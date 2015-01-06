package org.test.byteinspector.orchestration;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.test.byteinspector.model.MethodStatistics;
import org.test.byteinspector.repository.StatisticsRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Data analyzer thread responsible for running
 * clustering algorithm and produce the results.
 *
 * @author serkan
 */
public class AnalyzerManager extends Thread {

    // to scale all attribute values 0-NORMALIZATION_FACTOR range
    public static final Double NORMALIZATION_FACTOR = 100000d;

    @Override
    public void run() {
        System.out.println("Normalization started");
        normalize();
        System.out.println("Clustering started");
        cluster();
        System.out.println("Dumping results");
        dumpResults();
    }

    private void dumpResults() {
        Map<String, MethodStatistics> stats = StatisticsRepository.INSTANCE.getStats();
        FileWriter writer;
        try {
            writer = new FileWriter("/home/serkan/dev/results/clustering_dump.txt");
            for (Map.Entry<String, MethodStatistics> entry : stats.entrySet()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(entry.getKey());
                buffer.append("&");
                buffer.append(entry.getValue().getClusterId());
                buffer.append("&");
                MethodStatistics attributes = entry.getValue();
                for (Double val : attributes.values()) {
                    buffer.append(val);
                    buffer.append("&");
                }
                // end line char
                buffer.append("\n");
                writer.write(buffer.toString());
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * K-means clustering algorithm implemented.
     */
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
        String key = (String) stats.keySet().toArray()[0];
        MethodStatistics methodStatistics = stats.get(key);
        Set<String> attributeList = methodStatistics.keySet();
        List<MethodStatistics> centroids = randomK(clusterCount);
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
            Map<Integer, MethodStatistics> totals;
            totals = new HashMap<>(clusterCount);
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
                Integer clusterSize = clusterSizes.get(clusterId);
                if (clusterSize == 0) {
                    continue;
                }
                MethodStatistics clusterTotal = totalEntry.getValue();
                MethodStatistics newCentroid = new MethodStatistics("DUMMY");
                for (Map.Entry<String, Double> attributeEntry : clusterTotal.entrySet()) {
                    String attrName = attributeEntry.getKey();
                    Double attrVal = attributeEntry.getValue();
                    if (attrVal != 0) {
                        newCentroid.put(attrName, attrVal / clusterSize);
                    } else {
                        newCentroid.put(attrName, 0d);
                    }
                }
                newCentroid.setClusterId(clusterId);
                reCalculatedCentroids.add(newCentroid);
            }
        } while (!centroids.equals(reCalculatedCentroids));
        // find candidate clusters to send JIT
        // outlier clusters by invokeCount attribute
        // iterate centroids
        SummaryStatistics summaryStatistics = new SummaryStatistics();
        for (MethodStatistics centroid : reCalculatedCentroids) {
            Double invokeCount = centroid.get("invokeCount");
            summaryStatistics.addValue(invokeCount);
        }
        double upperLimit = summaryStatistics.getMean() + summaryStatistics.getStandardDeviation() * 2;
        List<Integer> candidateClusters = new LinkedList<>();
        System.out.println("-----------------Clusters : ------------------------");
        for (MethodStatistics centroid : reCalculatedCentroids) {
            System.out.println("Cluster ID : " + centroid.getClusterId() + " - " + clusterSizes.get(centroid.getClusterId()));

//            Double invokeCount = centroid.get("invokeCount");
//            if (invokeCount > upperLimit) {
//                candidateClusters.add(centroid.getClusterId());
//            }
        }
//        System.out.println("Cadidate Clusters : ");
//        for (Integer candidateCluster : candidateClusters) {
//            System.out.print(candidateCluster + ",");
//        }
    }

    /**
     * It compares distance between given data point
     * with centroids and assign it minimum distant
     * centroid's cluster.
     *
     * @param statistics data point
     * @param centroids  list of centroids
     */
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

    /**
     * Produces random k centroid. Instead of produce
     * random attribute values, select from real data points randomly.
     *
     * @param centroidCount how many centroid wanted
     * @return list of centroids
     */
    private List<MethodStatistics> randomK(int centroidCount) {
        Map<String, MethodStatistics> stats = StatisticsRepository.INSTANCE.getStats();
        Object[] objects = stats.keySet().toArray();
        List<MethodStatistics> centroids = new LinkedList<>();
        Random rnd = new Random();
        for (int i = 0; i < centroidCount; i++) {
            String key = (String) objects[rnd.nextInt(objects.length)];
            MethodStatistics methodStatistics = stats.get(key);
            // copy randomly selected point and change its name and clusterId
            MethodStatistics randomCent = new MethodStatistics("DUMMY");
            randomCent.putAll(methodStatistics);

            randomCent.setClusterId(i);
            centroids.add(randomCent);
        }
        return centroids;
    }

    /**
     * Calculates euclidean distance between
     * two method statistics objects. It should be
     * called on normalized values.
     *
     * @param m1 first data point
     * @param m2 second data point
     * @return distance between two data objects
     */
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

    /**
     * Normalize attributes values in range of 0-NORMALIZATION_FACTOR
     */
    private void normalize() {
        Map<String, Double> sum = new HashMap<>();
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
                methodStats.put(statName, (statValue / statSum) * NORMALIZATION_FACTOR);
            }
        }
    }
}
