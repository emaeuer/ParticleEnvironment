package de.emaeuer.ann.impl.layer.based;

import de.emaeuer.ann.ActivationFunction;
import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuronID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class NeuralNetworkLayerImpl {

    private LayerType type;

    private int layerIndex;

    private ActivationFunction activationFunction;

    private RealMatrix weights;
    private RealVector bias;
    private RealVector activation = null;

    private final List<NeuronID> neuronsOfLayer = new ArrayList<>();
    private final List<NeuronID> inputNeurons = new ArrayList<>();

    private NeuralNetworkImpl neuralNetwork;

    private final Map<NeuronID, List<NeuronID>> incomingConnections = new HashMap<>();
    private final Map<NeuronID, List<NeuronID>> outgoingConnections = new HashMap<>();

    private final NeuralNetworkLayerModifier modifier = new NeuralNetworkLayerModifier(this);

    private double maxWeight = Double.MAX_VALUE;
    private double minWeight = -Double.MAX_VALUE;

    public static NeuralNetworkLayerBuilderImpl build() {
        return new NeuralNetworkLayerBuilderImpl();
    }

    public RealVector process(RealVector externalInput) {
        if (!isInputLayer()) {
            throw new IllegalArgumentException("Only the input layer can process an external input vector");
        }
        return processVector(externalInput);
    }

    public RealVector process() {
        if (isInputLayer()) {
            throw new IllegalArgumentException("The input layer needs an input vector to process");
        }
        return processVector(buildInputVector());
    }

    private RealVector processVector(RealVector vector) {
        // the activation of the input layer is the result of the activation function applied to the external input
        RealVector output = switch (type) {
            case INPUT -> vector.map(this.activationFunction.getActivationFunction()::apply);
            case OUTPUT, HIDDEN -> applyWeightsBiasAndActivation(vector);
        };

        this.activation = output;
        return output;
    }

    private RealVector applyWeightsBiasAndActivation(RealVector vector) {
        RealVector weightedResult;
        if (this.weights == null) {
            // this layer has no incoming connections --> activation equals bias
            weightedResult = new ArrayRealVector(this.getNumberOfNeurons());
        } else {
            weightedResult = weights.operate(vector);
        }

        // bias is optional and can be realized by the use of an on neuron and its weights
        if (this.bias != null) {
            weightedResult = weightedResult.add(this.bias);
        }

        return weightedResult.map(this.activationFunction.getActivationFunction()::apply);
    }


    private RealVector buildInputVector() {
        return new ArrayRealVector(this.inputNeurons.stream()
                .mapToDouble(this.neuralNetwork::getLastActivationOf)
                .toArray());
    }

    public int getLayerIndex() {
        return this.layerIndex;
    }

    public void setLayerIndex(int id) {
        this.layerIndex = id;

        // refresh neuron layer index and corresponding map entries
        this.neuronsOfLayer.forEach(n -> modify().applyNeuronIDChange(n, new NeuronID(id, n.getNeuronIndex())));
    }

    public int getNumberOfNeurons() {
        // use dimension of activation because it always equal to the number of neurons and is initialized before list of neurons
        return this.activation.getDimension();
    }

    public boolean isInputLayer() {
        return this.type == LayerType.INPUT;
    }

    public boolean isOutputLayer() {
        return this.type == LayerType.OUTPUT;
    }

    public double getActivationOf(int inLayerID) {
        return this.activation.getEntry(inLayerID);
    }

    public RealVector getBias() {
        return this.bias;
    }

    public RealVector getActivation() {
        return this.activation;
    }

    public RealMatrix getWeights() {
        return this.weights;
    }

    public double getBiasOf(int inLayerID) {
        if (isInputLayer()) {
            throw new IllegalStateException("Neurons of the input layer have no bias");
        }
        return this.bias.getEntry(inLayerID);
    }

    public void setBiasOf(int inLayerID, double bias) {
        if (isInputLayer()) {
            throw new IllegalStateException("Can't change bias of input layer neuron");
        }

        bias = Math.max(this.minWeight, Math.min(this.maxWeight, bias));

        this.bias.setEntry(inLayerID, bias);
    }

    public double getWeightOf(NeuronID start, NeuronID end) {
        if (isInputLayer()) {
            throw new IllegalStateException("Connections to the input layer have no weight");
        } else if (end.getLayerIndex() != this.layerIndex) {
            throw new IllegalArgumentException(String.format("Can't retrieve weight of the connection from %s to %s in layer %d", start, end, this.layerIndex));
        }

        int indexOfInput = this.inputNeurons.indexOf(start);

        if (indexOfInput == -1) {
            throw new IllegalArgumentException(String.format("The connection from %s to %s doesn't exist", start, end));
        }

        return this.weights.getEntry(end.getNeuronIndex(), indexOfInput);
    }

    public void setWeightOf(NeuronID start, NeuronID end, double weight) {
        if (isInputLayer()) {
            throw new IllegalStateException("Can't change weight of connection to the input layer");
        } else if (end.getLayerIndex() != this.layerIndex) {
            throw new IllegalArgumentException(String.format("Can't change weight of the connection from %s to %s in layer %d", start, end, this.layerIndex));
        }

        int indexOfInput = this.inputNeurons.indexOf(start);

        if (indexOfInput == -1) {
            throw new IllegalArgumentException(String.format("The connection from %s to %s doesn't exist", start, end));
        }

        weight = Math.max(this.minWeight, Math.min(this.maxWeight, weight));

        this.weights.setEntry(end.getNeuronIndex(), indexOfInput, weight);
    }

    public NeuralNetworkLayerModifier modify() {
        return this.modifier;
    }

    public ActivationFunction getActivationFunction() {
        return this.activationFunction;
    }

    public void setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = activationFunction;
    }

    public void setBias(RealVector bias) {
        this.bias = bias;
    }

    public void setActivation(RealVector activation) {
        this.activation = activation;
    }

    public void setLayerType(LayerType type) {
        this.type = type;
    }

    public NeuralNetworkImpl getNeuralNetwork() {
        return this.neuralNetwork;
    }

    public List<NeuronID> getOutgoingConnectionsOfNeuron(NeuronID neuron) {
        return this.outgoingConnections.getOrDefault(neuron, Collections.emptyList());
    }

    public Map<NeuronID, List<NeuronID>> getOutgoingConnections() {
        return this.outgoingConnections;
    }

    public List<NeuronID> getIncomingConnectionsOfNeuron(NeuronID neuron) {
        return this.incomingConnections.getOrDefault(neuron, Collections.emptyList());
    }

    public Map<NeuronID, List<NeuronID>> getIncomingConnections() {
        return this.incomingConnections;
    }

    public void setNeuralNetwork(NeuralNetworkImpl neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }

    public void setWeights(RealMatrix weights) {
        this.weights = weights;
    }

    public List<NeuronID> getNeurons() {
        return this.neuronsOfLayer;
    }

    public LayerType getType() {
        return this.type;
    }

    public List<NeuronID> getInputNeurons() {
        return this.inputNeurons;
    }

    public void addOutgoingConnection(NeuronID start, NeuronID end) {
        if (start.getLayerIndex() != this.layerIndex) {
            throw new UnsupportedOperationException(String.format("Can't add outgoing connection from %s to %s to layer %d", start, end, this.layerIndex));
        }

        this.getOutgoingConnections().putIfAbsent(start, new ArrayList<>());
        this.getOutgoingConnectionsOfNeuron(start).add(end);
    }

    public void addIncomingConnection(NeuronID start, NeuronID end) {
        if (end.getLayerIndex() != this.layerIndex) {
            throw new UnsupportedOperationException(String.format("Can't add incoming connection from %s to %s to layer %d", start, end, this.layerIndex));
        }

        this.getIncomingConnections().putIfAbsent(end, new ArrayList<>());
        this.getIncomingConnectionsOfNeuron(end).add(start);
    }

    public NeuralNetworkLayerImpl copy(NeuralNetworkImpl copyNn, Map<NeuronID, NeuronID> existingNeurons) {
        NeuralNetworkLayerImpl copy = new NeuralNetworkLayerImpl();
        copy.setLayerIndex(this.layerIndex);
        copy.setLayerType(this.type);
        copy.setActivationFunction(this.activationFunction);
        copy.setWeights(this.weights == null ? null : this.weights.copy());
        copy.setBias(this.bias == null ? null : this.bias.copy());
        copy.setNeuralNetwork(copyNn);
        copy.setActivation(new ArrayRealVector(this.getNumberOfNeurons())); // activations are not copied
        copy.setMaxWeight(this.maxWeight);
        copy.setMinWeight(this.minWeight);
        // don't set activation because copy returns a neural network in the initial state

        copyNeuronCollection(existingNeurons, this.neuronsOfLayer, copy.neuronsOfLayer);
        copyNeuronCollection(existingNeurons, this.inputNeurons, copy.inputNeurons);
        copyNeuronCollection(existingNeurons, this.incomingConnections, copy.incomingConnections);
        copyNeuronCollection(existingNeurons, this.outgoingConnections, copy.outgoingConnections);

        return copy;
    }

    private void copyNeuronCollection(Map<NeuronID, NeuronID> existingNeurons, List<NeuronID> source, List<NeuronID> target) {
        // copies the neuron or uses an already existing object if present
        // map is used instead of set to easily retrieve the exact reference of the existing neuron
        source.stream()
                .map(n -> existingNeurons.getOrDefault(n, new NeuronID(n.getLayerIndex(), n.getNeuronIndex())))
                .peek(n -> existingNeurons.putIfAbsent(n, n))
                .forEach(target::add);
    }

    private void copyNeuronCollection(Map<NeuronID, NeuronID> existingNeurons, Map<NeuronID, List<NeuronID>> source, Map<NeuronID, List<NeuronID>> target) {
        // copies the neuron or uses an already existing object if present
        for (Entry<NeuronID, List<NeuronID>> connectionsOfNeuron : source.entrySet()) {
            NeuronID neuron = existingNeurons.getOrDefault(connectionsOfNeuron.getKey(),
                    new NeuronID(connectionsOfNeuron.getKey().getLayerIndex(), connectionsOfNeuron.getKey().getNeuronIndex()));
            existingNeurons.putIfAbsent(neuron, neuron);

            List<NeuronID> connectionsCopy = connectionsOfNeuron.getValue().stream()
                    .map(n -> existingNeurons.getOrDefault(n, new NeuronID(n.getLayerIndex(), n.getNeuronIndex())))
                    .peek(n -> existingNeurons.putIfAbsent(n, n))
                    .collect(Collectors.toCollection(ArrayList::new));

            target.put(neuron, connectionsCopy);
        }
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double value) {
        this.maxWeight = value;
    }

    public double getMinWeight() {
        return minWeight;
    }

    public void setMinWeight(double value) {
        this.minWeight = value;
    }
}
