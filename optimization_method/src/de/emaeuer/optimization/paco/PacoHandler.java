package de.emaeuer.optimization.paco;

import de.emaeuer.ann.NeuralNetwork;
import de.emaeuer.ann.configuration.NeuralNetworkConfiguration;
import de.emaeuer.configuration.ConfigurationHandler;
import de.emaeuer.configuration.ConfigurationHelper;
import de.emaeuer.optimization.OptimizationMethod;
import de.emaeuer.optimization.Solution;
import de.emaeuer.optimization.configuration.OptimizationConfiguration;
import de.emaeuer.optimization.configuration.OptimizationState;
import de.emaeuer.optimization.paco.configuration.PacoConfiguration;
import de.emaeuer.optimization.paco.pheromone.AbstractPopulationBasedPheromone;
import de.emaeuer.optimization.paco.pheromone.AgePopulationBasedPheromone;
import de.emaeuer.optimization.paco.pheromone.FitnessPopulationBasedPheromone;
import de.emaeuer.state.StateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.IntStream;

import static de.emaeuer.optimization.paco.configuration.PacoConfiguration.*;

public class PacoHandler extends OptimizationMethod {

    private final static Logger LOG = LogManager.getLogger(PacoHandler.class);

    private final AbstractPopulationBasedPheromone pheromone;

    private final List<PacoAnt> currentAnts = new ArrayList<>();

    private final ConfigurationHandler<PacoConfiguration> configuration;

    public PacoHandler(ConfigurationHandler<OptimizationConfiguration> configuration, StateHandler<OptimizationState> generalState) {
        super(configuration, generalState);

        this.configuration = ConfigurationHelper.extractEmbeddedConfiguration(configuration, PacoConfiguration.class, OptimizationConfiguration.OPTIMIZATION_CONFIGURATION);

        // build basic neural network with just the necessary network neurons and connections
        NeuralNetwork baseNetwork = NeuralNetwork.build()
                .configure(ConfigurationHelper.extractEmbeddedConfiguration(configuration, NeuralNetworkConfiguration.class, OptimizationConfiguration.OPTIMIZATION_NEURAL_NETWORK_CONFIGURATION))
                .implicitBias()
                .inputLayer()
                .fullyConnectToNextLayer()
                .outputLayer()
                .finish();

        if (this.configuration.getValue(REMOVE_WORST, Boolean.class)) {
            this.pheromone = new FitnessPopulationBasedPheromone(this.configuration, baseNetwork);
        } else {
            this.pheromone = new AgePopulationBasedPheromone(this.configuration, baseNetwork);
        }
    }

    @Override
    protected List<? extends Solution> generateSolutions() {
        this.currentAnts.clear();

        if (!this.pheromone.getPopulation().isEmpty() && configuration.getValue(KEEP_BEST, Boolean.class)) {
            this.currentAnts.add(new PacoAnt(this.pheromone.createNeuralNetworkForGlobalBest()));
        }

        // if pheromone matrix is empty create the necessary number of ants to fill the population completely
        int antsPerIteration = this.pheromone.getPopulation().isEmpty()
                ? this.configuration.getValue(POPULATION_SIZE, Integer.class)
                : this.configuration.getValue(ANTS_PER_ITERATION, Integer.class);

        IntStream.range(this.currentAnts.size(), antsPerIteration)
                .mapToObj(i -> this.pheromone.createNeuralNetworkForPheromone())
                .map(PacoAnt::new)
                .forEach(this.currentAnts::add);

        return this.currentAnts;
    }

    @Override
    public void update() {
        PacoAnt bestOfThisIteration;
        if (this.pheromone.getPopulation().isEmpty()) {
            // initially all ant update regardless of the fitness to fill the population
            bestOfThisIteration = this.currentAnts.stream()
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        } else {
            bestOfThisIteration = this.currentAnts.stream()
                    .sorted(Comparator.comparingDouble(PacoAnt::getFitness))
                    .skip(this.currentAnts.size() - this.configuration.getValue(UPDATES_PER_ITERATION, Integer.class))
                    .peek(this.pheromone::addAntToPopulation)
                    .max(Comparator.comparingDouble(PacoAnt::getFitness))
                    .orElse(null);
        }

        if (bestOfThisIteration != null) {
            // copy best to prevent further modification because of references in pheromone matrix
            PacoAnt bestCopy = new PacoAnt(bestOfThisIteration.getNeuralNetwork().copy());
            bestCopy.setFitness(bestOfThisIteration.getFitness());
            setCurrentlyBestSolution(bestCopy);
        }

        super.update();
    }

    @Override
    protected void handleProgressionStagnation() {
        super.handleProgressionStagnation();
        this.pheromone.increaseComplexity();
    }

    @Override
    protected DoubleSummaryStatistics getFitnessOfIteration() {
        return this.currentAnts
                .stream()
                .mapToDouble(PacoAnt::getFitness)
                .summaryStatistics();
    }
}
