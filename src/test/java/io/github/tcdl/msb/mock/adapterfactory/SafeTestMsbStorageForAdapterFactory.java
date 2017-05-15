package io.github.tcdl.msb.mock.adapterfactory;

import io.github.tcdl.msb.api.MsbContext;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SafeTestMsbStorageForAdapterFactory extends TestMsbStorageForAdapterFactory {
    private final ConcurrentHashMap<String, TestMsbConsumerAdapter> consumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TestMsbProducerAdapter> producers = new ConcurrentHashMap<>();

    @Override
    public void connect(MsbContext otherContext) {
        throw new UnsupportedOperationException("don't think we need this.");
    }

    @Override
    void addProducerAdapter(String namespace, TestMsbProducerAdapter adapter) {
        producers.put(namespace, adapter);
    }

    @Override
    void addConsumerAdapter(String namespace, TestMsbConsumerAdapter adapter) {
        consumers.put(namespace, adapter);
    }

    @Override
    void addPublishedTestMessage(String namespace, String jsonMessage) {
        // there's no need for this to do anything
    }

    @Override
    public synchronized void cleanup() {
        consumers.clear();
        producers.clear();
    }

    @Override
    public void publishIncomingMessage(String namespace, String jsonMessage) {
        TestMsbConsumerAdapter consumerAdapter = consumers.get(namespace);
        if(consumerAdapter != null) {
            consumerAdapter.pushTestMessage(jsonMessage);
        }
    }

    @Override
    public List<String> getOutgoingMessages(String namespace) {
        throw new UnsupportedOperationException("don't think we need this.");
    }

    @Override
    public String getOutgoingMessage(String namespace) {
        throw new UnsupportedOperationException("don't think we need this.");
    }

}