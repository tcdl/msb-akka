package io.github.tcdl.msb.mock.adapterfactory;

import io.github.tcdl.msb.adapters.AdapterFactory;
import io.github.tcdl.msb.adapters.ConsumerAdapter;
import io.github.tcdl.msb.adapters.ProducerAdapter;
import io.github.tcdl.msb.api.RequestOptions;
import io.github.tcdl.msb.api.ResponderOptions;
import io.github.tcdl.msb.config.MsbConfig;

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
    public ProducerAdapter createProducerAdapter(String topic, RequestOptions requestOptions) {
        TestMsbProducerAdapter producerAdapter = new TestMsbProducerAdapter(topic, storage);
        storage.addProducerAdapter(topic, producerAdapter);
        return producerAdapter;
    }

    @Override
    public ConsumerAdapter createConsumerAdapter(String namespace, boolean isResponseTopic) {
        SafeTestMsbConsumerAdapter consumerAdapter = new SafeTestMsbConsumerAdapter(namespace, storage);
        storage.addConsumerAdapter(namespace, consumerAdapter);
        return consumerAdapter;
    }

    @Override
    public ConsumerAdapter createConsumerAdapter(String topic, ResponderOptions responderOptions, boolean b) {
        ResponderOptions effectiveResponderOptions = responderOptions != null ? responderOptions: ResponderOptions.DEFAULTS;
        SafeTestMsbConsumerAdapter consumerAdapter = new SafeTestMsbConsumerAdapter(topic, storage);
        storage.addConsumerAdapter(topic, effectiveResponderOptions.getBindingKeys(), consumerAdapter);
        return consumerAdapter;
    }

    @Override
    public boolean isUseMsbThreadingModel() {
        return false;
    }

    @Override
    public void shutdown() {

    }
}
