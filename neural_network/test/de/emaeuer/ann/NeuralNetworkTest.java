package de.emaeuer.ann;

import de.emaeuer.ann.impl.layer.based.NeuralNetworkBuilderImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkImpl;
import de.emaeuer.ann.impl.layer.based.NeuralNetworkLayerImpl;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NeuralNetworkTest {

    /*
     ##########################################################
     ################# Data creation Methods ##################
     ##########################################################
    */

    private NeuralNetwork buildNeuralNetwork(int... numberOfNeurons) {
        if (numberOfNeurons.length < 2) {
            fail("A neural network needs at least 2 layers");
        }
        NeuralNetworkBuilderImpl builder = (NeuralNetworkBuilderImpl) NeuralNetwork.build()
                .inputLayer(numberOfNeurons[0])
                .fullyConnectToNextLayer();

        for (int i = 1; i < numberOfNeurons.length - 1; i++) {
            builder = builder.hiddenLayer(numberOfNeurons[i])
                    .fullyConnectToNextLayer();
        }

        NeuralNetworkImpl nn = (NeuralNetworkImpl) builder.outputLayer(numberOfNeurons[numberOfNeurons.length - 1])
                .finish();

        // set all weights to -1 (for comparison with new weights)
        for (NeuralNetworkLayerImpl layer : nn.getLayers()) {
            if (!layer.isInputLayer()) {
                layer.setWeights(layer.getWeights().scalarAdd(-1));
            }
        }

        return nn;
    }

    /*
     ##########################################################
     ##################### Test Methods #######################
     ##########################################################
    */

    @Test
    public void testXOR() {
        NeuralNetworkImpl nn = (NeuralNetworkImpl) NeuralNetwork.build()
                .inputLayer(2)
                .hiddenLayer(b -> b.numberOfNeurons(2)
                        .activationFunction(ActivationFunction.RELU)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 0), 1)
                        .addConnection(new NeuronID(0, 0), new NeuronID(1, 1), -1)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 0), -1)
                        .addConnection(new NeuronID(0, 1), new NeuronID(1, 1), 1))
                .outputLayer(b -> b.numberOfNeurons(1)
                        .activationFunction(ActivationFunction.RELU)
                        .addConnection(new NeuronID(1, 0), new NeuronID(2, 0), 1)
                        .addConnection(new NeuronID(1, 1), new NeuronID(2, 0), 1))
                .finish();

        RealVector input = new ArrayRealVector(2);

        // Test 0 ^ 0
        input.setEntry(0, 0);
        input.setEntry(1, 0);
        assertArrayEquals(new double[]{0}, nn.process(input).toArray());
        assertArrayEquals(new double[]{0, 0}, nn.getLayer(1).getActivation().toArray());

        // Test 0 ^ 1
        input.setEntry(0, 0);
        input.setEntry(1, 1);
        assertArrayEquals(new double[]{1}, nn.process(input).toArray());
        assertArrayEquals(new double[]{0, 1}, nn.getLayer(1).getActivation().toArray());
        // Test 1 ^ 0
        input.setEntry(0, 1);
        input.setEntry(1, 0);
        assertArrayEquals(new double[]{1}, nn.process(input).toArray());
        assertArrayEquals(new double[]{1, 0}, nn.getLayer(1).getActivation().toArray());
        // Test 1 ^ 1
        input.setEntry(0, 1);
        input.setEntry(1, 1);
        assertArrayEquals(new double[]{0}, nn.process(input).toArray());
        assertArrayEquals(new double[]{0, 0}, nn.getLayer(1).getActivation().toArray());
    }

    @Test
    public void testCopy() {
        NeuralNetworkImpl nn = (NeuralNetworkImpl) buildNeuralNetwork(2, 3, 2, 2);

        // add all kinds of connections
        nn.modify()
                .addConnection(new NeuronID(1, 2), new NeuronID(3, 0), 1)
                .addConnection(new NeuronID(3, 1), new NeuronID(1, 0), 1)
                .addConnection(new NeuronID(2, 1), new NeuronID(2, 0), 1)
                .addConnection(new NeuronID(2, 0), new NeuronID(2, 0), 1);

        // copy neural network
        NeuralNetworkImpl copy = (NeuralNetworkImpl) nn.copy();

        // check general neural network
        assertNotSame(nn, copy);
        assertEquals(nn.getDepth(), copy.getDepth());

        // check layers, neurons and connections
        for (int i = 0; i < nn.getDepth(); i++) {
            NeuralNetworkLayerImpl layer = nn.getLayer(i);
            NeuralNetworkLayerImpl copyLayer = copy.getLayer(i);

            // compare layers
            assertArrayEquals(layer.getInputNeurons().toArray(), copyLayer.getInputNeurons().toArray());
            assertArrayEquals(layer.getNeurons().toArray(), copyLayer.getNeurons().toArray());
            assertEquals(layer.getBias(), copyLayer.getBias());
            assertEquals(layer.getWeights(), copyLayer.getWeights());
            assertEquals(layer.getLayerIndex(), copyLayer.getLayerIndex());
            assertEquals(layer.getType(), copyLayer.getType());
            assertEquals(layer.getActivationFunction(), copyLayer.getActivationFunction());
            assertEquals(0, copyLayer.getActivation() == null ? 0 : copyLayer.getActivation().getL1Norm());
            assertNotSame(layer, copyLayer);

            // compare neurons and connections
            for (int j = 0; j < layer.getNumberOfNeurons(); j++) {
                NeuronID neuron = layer.getNeurons().get(j);
                NeuronID copyNeuron = copyLayer.getNeurons().get(j);

                assertEquals(neuron, copyNeuron);
                assertArrayEquals(nn.getOutgoingConnectionsOfNeuron(neuron).toArray(), copy.getOutgoingConnectionsOfNeuron(copyNeuron).toArray());
                assertArrayEquals(nn.getIncomingConnectionsOfNeuron(neuron).toArray(), copy.getIncomingConnectionsOfNeuron(copyNeuron).toArray());
                assertNotSame(neuron, copyNeuron);
            }
        }
    }

    @Test
    public void testIsolatedNeurons() {
        NeuralNetworkImpl nn = (NeuralNetworkImpl) buildNeuralNetwork(2, 2);
        nn.modify()
                .removeConnection(new NeuronID(0, 0), new NeuronID(1, 0))
                .removeConnection(new NeuronID(0, 0), new NeuronID(1, 1))
                .setBiasOfNeuron(new NeuronID(1, 0), 1);

        assertFalse(nn.neuronHasConnectionTo(new NeuronID(0, 0), new NeuronID(1, 0)));
        assertFalse(nn.neuronHasConnectionTo(new NeuronID(0, 0), new NeuronID(1, 1)));
        assertFalse(nn.neuronHasConnectionToLayer(new NeuronID(0, 0), 1));

        // activation equals bias
        assertArrayEquals(new double[] {0, -1}, nn.process(new ArrayRealVector(new double[] {1, 1})).toArray());

        nn.modify().addConnection(new NeuronID(0, 0), new NeuronID(1, 0), 1);

        assertEquals(1, nn.getWeightOfConnection(new NeuronID(0, 0), new NeuronID(1, 0)));
        assertTrue(nn.neuronHasConnectionTo(new NeuronID(0, 0), new NeuronID(1, 0)));
        assertTrue(nn.neuronHasConnectionToLayer(new NeuronID(0, 0), 1));

        // activation equals input + bias
        assertArrayEquals(new double[] {1, -1}, nn.process(new ArrayRealVector(new double[] {1, 1})).toArray());
    }

    /*
     ##########################################################
     #################### Helper Methods ######################
     ##########################################################
    */

}
