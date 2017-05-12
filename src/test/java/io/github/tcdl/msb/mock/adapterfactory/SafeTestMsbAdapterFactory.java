package io.github.tcdl.msb.mock.adapterfactory;

import io.github.tcdl.msb.adapters.AdapterFactory;
import io.github.tcdl.msb.adapters.ConsumerAdapter;
import io.github.tcdl.msb.adapters.MessageHandlerInvokeStrategy;
import io.github.tcdl.msb.adapters.ProducerAdapter;
import io.github.tcdl.msb.config.MsbConfig;
import io.github.tcdl.msb.impl.SimpleMessageHandlerInvokeStrategyImpl;

/**
 * This class is for testing purposes only and is intended as a more multithreading friendly alternative to
 * {@link TestMsbAdapterFactory}.
 */
public class SafeTestMsbAdapterFactory implements AdapterFactory {

    private SafeTestMsbStorageForAdapterFactory storage = new SafeTestMsbStorageForAdapterFactory();

    @Override
    public void init(MsbConfig msbConfig) {

    }

    @Override
    public ProducerAdapter createProducerAdapter(String namespace) {
        TestMsbProducerAdapter producerAdapter = new TestMsbProducerAdapter(namespace, storage);
        storage.addProducerAdapter(namespace, producerAdapter);
        return producerAdapter;
    }

    @Override
    public ConsumerAdapter createConsumerAdapter(String namespace, boolean isResponseTopic) {
        SafeTestMsbConsumerAdapter consumerAdapter = new SafeTestMsbConsumerAdapter(namespace, storage);
        storage.addConsumerAdapter(namespace, consumerAdapter);
        return consumerAdapter;
    }

    @Override
    public MessageHandlerInvokeStrategy createMessageHandlerInvokeStrategy(String topic) {
        return new SimpleMessageHandlerInvokeStrategyImpl();
    }

    @Override
    public void shutdown() {

    }
}
