package de.uni.environment;

import de.uni.environment.elements.Particle;
import de.uni.environment.util.EnvironmentHelper;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractEnvironment {

    private final IntegerProperty particleNumber = new SimpleIntegerProperty();

    private final DoubleProperty width = new SimpleDoubleProperty(800);
    private final DoubleProperty height = new SimpleDoubleProperty(800);

    private final List<Particle> particles = new ArrayList<>();

    private final BiConsumer<Particle, AbstractEnvironment> borderStrategy;

    public AbstractEnvironment(int particleNumber) {
        this(particleNumber, EnvironmentHelper.GO_TO_OTHER_SIDE);
    }

    public AbstractEnvironment(int particleNumber, BiConsumer<Particle, AbstractEnvironment> borderStrategy) {
        this.borderStrategy = borderStrategy;
        setParticleNumber(particleNumber);

        initialize();

        initializeParticles();
    }

    protected abstract void initialize();

    protected abstract void initializeParticles();

    public void update() {
        this.particles.stream()
                .peek(Particle::step)
                .forEach(p -> this.borderStrategy.accept(p, this));
    }

    public double getWidth() {
        return width.get();
    }

    public DoubleProperty widthProperty() {
        return width;
    }

    public void setWidth(double width) {
        this.width.set(width);
    }

    public double getHeight() {
        return height.get();
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    public void setHeight(double height) {
        this.height.set(height);
    }

    public int getParticleNumber() {
        return particleNumber.get();
    }

    public IntegerProperty particleNumberProperty() {
        return particleNumber;
    }

    public void setParticleNumber(int particleNumber) {
        this.particleNumber.set(particleNumber);
    }

    public List<Particle> getParticles() {
        return particles;
    }
}
