package de.emaeuer.state.value;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapOfStateValue extends AbstractStateValue<Entry<String, AbstractStateValue<?, ?>>, Map<String, AbstractStateValue<?, ?>>> {

    private final Map<String, AbstractStateValue<?,?>> value = new HashMap<>();

    @Override
    public Class<? extends Entry<String, AbstractStateValue<?, ?>>> getExpectedInputType() {
        Class<?> type = Entry.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Entry<String, AbstractStateValue<?, ?>>>) type;
    }

    @Override
    public Class<? extends Map<String, AbstractStateValue<?, ?>>> getOutputType() {
        Class<?> type = Map.class;
        //noinspection unchecked only way to return class with generic is unsafe cast
        return (Class<? extends Map<String, AbstractStateValue<?, ?>>>) type;
    }

    @Override
    protected void handleNewValue(Entry<String, AbstractStateValue<?, ?>> value) {
        if (value instanceof EmbeddedState || value instanceof MapOfStateValue) {
            throw new UnsupportedOperationException("MapOfStateValue doesn't support selection of embedded states or further selections");
        }

        this.value.put(value.getKey(), value.getValue());
    }

    @Override
    public Map<String, AbstractStateValue<?,?>> getValueImpl() {
        return this.value;
    }
}
