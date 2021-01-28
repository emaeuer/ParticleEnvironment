package de.emaeuer.ann.impl;

import de.emaeuer.ann.LayerType;
import de.emaeuer.ann.NeuralNetworkModifier;
import de.emaeuer.ann.NeuronID;

public class NeuralNetworkModifierImpl implements NeuralNetworkModifier {

    private final NeuralNetworkImpl nn;

    public NeuralNetworkModifierImpl(NeuralNetworkImpl nn) {
        this.nn = nn;
    }

    @Override
    public NeuralNetworkModifierImpl splitConnection(NeuronID start, NeuronID end) {
        start = getReferenceToCorrespondingNeuronID(start);
        end = getReferenceToCorrespondingNeuronID(end);

        // initialize intermediateNeuron depending on the positions of start and end
        int layerDistance = end.getLayerIndex() - start.getLayerIndex();
        NeuronID intermediateNeuron = Math.abs(layerDistance) == 1
            ? insertNewLayerWithIntermediateNeuron(start, end, layerDistance)
            : insertIntermediateNeuronToExistingLayer(start, end, layerDistance);

        // replace old connection with two new ones --> configure weights in a way that the new connections are equivalent to the old one
        // connection from start to intermediateNeuron was already inserted with the weight of the old connection
        addConnection(intermediateNeuron, end, 1);
        removeConnection(start, end);

         return this;
    }

    private NeuronID insertIntermediateNeuronToExistingLayer(NeuronID start, NeuronID end, int layerDistance) {
        // Signum ==  0 --> Neurons are in the same layer (lateral connection) --> add new neuron between them
        // Signum ==  1 --> at least one layer between the neurons (forward connection) --> add neuron to first layer after start
        // Signum == -1 --> at least one layer between the neurons (recurrent connection) --> add neuron to first layer before start
        NeuralNetworkLayerImpl layer = this.nn.getLayers()
                .get(start.getLayerIndex() + Integer.signum(layerDistance));

        NeuronID newEnd = new NeuronID(layer.getLayerIndex(), layer.getNumberOfNeurons());
        layer.modify()
                .addNeuron(0)
                .addConnection(start, newEnd, this.nn.getWeightOfConnection(start, end));
        return newEnd;
    }

    private NeuronID insertNewLayerWithIntermediateNeuron(NeuronID start, NeuronID end, int layerDistance) {
        // Signum ==  1 --> Neurons are in consecutive layers (forward connection) --> add new layer between them
        // Signum == -1 --> Neurons are in consecutive layers (recurrent connection) --> add new layer between them
        // use max(0, layerDistance) because for a recurrent connection (-1) the start and not the output layer should be shifted
        double connectionWeight = this.nn.getWeightOfConnection(start, end);
        NeuralNetworkLayerImpl layer = addNewLayerAtPosition(start.getLayerIndex() + Math.max(0, layerDistance), start, connectionWeight);
        return new NeuronID(layer.getLayerIndex(), 0); // neuron is only neuron of this layer
    }

    /**
     * Creates a new {@link NeuralNetworkLayerImpl} with one neuron and a connection to it
     * @param position id of the new layer
     * @param start start neuron of the only connection to this layer
     * @param connectionWeight weight of the connection to this layer
     */
    private NeuralNetworkLayerImpl addNewLayerAtPosition(int position, NeuronID start, double connectionWeight) {
        // increase indices of all following layers by one
        this.nn.getLayers().subList(position, this.nn.getLayers().size())
                .forEach(layer -> layer.setLayerIndex(layer.getLayerIndex() + 1));

        // add placeholder at the position of the new layer so that the indexing after shifting still works
        this.nn.getLayers().add(position, null);

        NeuralNetworkLayerImpl newLayer = NeuralNetworkLayerImpl.build()
                .numberOfNeurons(1)
                .layerType(LayerType.HIDDEN)
                .neuralNetwork(this.nn)
                .layerID(position)
                .addConnection(start, new NeuronID(position, 0), connectionWeight)
                .finish();

        this.nn.getLayers().set(position, newLayer);

        return newLayer;
    }

    @Override
    public NeuralNetworkModifierImpl addConnection(NeuronID startID, NeuronID endID, double weight) {
        startID = getReferenceToCorrespondingNeuronID(startID);
        endID = getReferenceToCorrespondingNeuronID(endID);

        this.nn.getLayer(endID.getLayerIndex())
                .modify()
                .addConnection(startID, endID, weight);

        return this;
    }

    @Override
    public NeuralNetworkModifierImpl removeConnection(NeuronID startID, NeuronID endID) {
        startID = getReferenceToCorrespondingNeuronID(startID);
        endID = getReferenceToCorrespondingNeuronID(endID);

        this.nn.getLayer(endID.getLayerIndex())
                .modify()
                .removeConnection(startID, endID);

        return this;
    }

    @Override
    public NeuralNetworkModifierImpl addNeuron(int layerID, double bias) {
        this.nn.getLayer(layerID)
                .modify()
                .addNeuron(bias);

        return this;
    }

    @Override
    public NeuralNetworkModifierImpl removeNeuron(NeuronID neuron) {
        neuron = getReferenceToCorrespondingNeuronID(neuron);
        this.nn.getLayer(neuron.getLayerIndex())
                .modify()
                .removeNeuron(neuron);

        return this;
    }

    /**
     * Finds the corresponding {@link NeuronID} object int the neural network and returns the reference to this one
     * @param other NeuronID
     * @return reference to the corresponding {@link NeuronID} in this network
     */
    private NeuronID getReferenceToCorrespondingNeuronID(NeuronID other) {
        return this.nn.getLayer(other.getLayerIndex()).getNeurons().get(other.getNeuronIndex());
    }
}
