package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.TopologyData;
import org.apache.commons.math3.linear.RealVector;

public class PacoAnt implements Solution {

    private double fitness = 0;

    private final TopologyData solution;

    public PacoAnt(NeuralNetwork brain, int topologyGroupID) {
        this.solution = new TopologyData(brain, topologyGroupID);
    }

    public PacoAnt(TopologyData topology) {
        this.solution = topology;
    }

    @Override
    public RealVector process(RealVector input) {
        return this.solution.getInstance().process(input);
    }

    public double getFitness() {
        return this.fitness;
    }

    @Override
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public NeuralNetwork getNeuralNetwork() {
        return solution.getInstance();
    }

    public TopologyData getTopologyData() {
        return this.solution;
    }
}
