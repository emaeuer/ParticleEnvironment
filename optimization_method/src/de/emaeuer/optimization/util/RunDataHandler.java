package de.emaeuer.optimization.util;

import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.state.StateHandler;

import java.util.*;
import java.util.stream.IntStream;

public class RunDataHandler {

    public record RunSummary(double fitness, int iterations, int hiddenNodes, int connections) {}

    private static class SimpleDoubleStatistic {
        private double sum = 0;
        private double count = 0;

        public void addValue(double value) {
            count++;
            sum += value;
        }

        public double getAverage() {
            return sum / count;
        }
    }

    private final DoubleSummaryStatistics maxFitness = new DoubleSummaryStatistics();

    private final IntSummaryStatistics neededIterationNumbers = new IntSummaryStatistics();
    private final IntSummaryStatistics hiddenNodeNumber = new IntSummaryStatistics();
    private final IntSummaryStatistics connectionNumber = new IntSummaryStatistics();

    private final Map<Integer, SimpleDoubleStatistic> fitnessSeries = new HashMap<>();
    private final List<Integer> evaluationValues = new ArrayList<>();

    private int evaluationIndex = 0;
    private int numberOfFinishedRuns = 0;

    private final StateHandler<OptimizationState> generalState;

    public RunDataHandler(StateHandler<OptimizationState> state) {
        this.generalState = state;
    }

    public void addSummaryOfRun(RunSummary summary) {
        finishSeries();

        this.maxFitness.accept(summary.fitness());
        this.neededIterationNumbers.accept(summary.iterations());
        this.hiddenNodeNumber.accept(summary.hiddenNodes());
        this.connectionNumber.accept(summary.connections());

        generalState.addNewValue(OptimizationState.ITERATION_DISTRIBUTION, this.neededIterationNumbers.getAverage());
        generalState.addNewValue(OptimizationState.HIDDEN_NODES_DISTRIBUTION, this.hiddenNodeNumber.getAverage());
        generalState.addNewValue(OptimizationState.CONNECTIONS_DISTRIBUTION, this.connectionNumber.getAverage());
        generalState.addNewValue(OptimizationState.FITNESS_DISTRIBUTION, this.maxFitness.getAverage());
    }

    public void addFitnessSummary(int evaluationCount, DoubleSummaryStatistics statistic) {
        SimpleDoubleStatistic average;

        if (!this.fitnessSeries.containsKey(evaluationCount)) {
            // no previous run needed that many evaluations --> fitness at this iteration was max value
            average = new SimpleDoubleStatistic();
            // FIXME if the max fitness score is not 10000 the result is wrong
            IntStream.range(0, this.numberOfFinishedRuns)
                    .forEach(i -> average.addValue(10000));
            this.fitnessSeries.put(evaluationCount, average);
            this.evaluationValues.add(evaluationCount);
        } else {
            average = this.fitnessSeries.get(evaluationCount);
        }

        average.addValue(statistic.getMax());

        this.generalState.addNewValue(OptimizationState.AVERAGE_RUN_FITNESS_SERIES, new AbstractMap.SimpleEntry<>("Average max fitness", new Double[] {(double) evaluationCount, average.getAverage()}));
        this.evaluationIndex++;
    }

    private void finishSeries() {
        // if run finished before others add value of max fitness to all following values
        for (int i = this.evaluationIndex; i < this.evaluationValues.size(); i++) {
            int evaluationCount = this.evaluationValues.get(i);
            SimpleDoubleStatistic average = this.fitnessSeries.get(evaluationCount);
            // FIXME if the max fitness score is not 10000 the result is wrong
            average.addValue(10000);
            this.generalState.addNewValue(OptimizationState.AVERAGE_RUN_FITNESS_SERIES, new AbstractMap.SimpleEntry<>("Average max fitness", new Double[] {(double) evaluationCount, average.getAverage()}));
        }

        this.evaluationIndex = 0;
        this.numberOfFinishedRuns++;
    }
}
