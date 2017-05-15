package io.github.tcdl.msb.mock.adapterfactory;

import io.github.tcdl.msb.acknowledge.AcknowledgementHandlerInternal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

public class SafeTestMsbConsumerAdapter extends TestMsbConsumerAdapter {

    private final Set<RawMessageHandler> rawMessageHandlers = newSetFromMap(new ConcurrentHashMap<>());

    public SafeTestMsbConsumerAdapter(String namespace, TestMsbStorageForAdapterFactory storage) {
        super(namespace, storage);
    }

    @Override
    public void subscribe(RawMessageHandler onMessageHandler) {
        rawMessageHandlers.add(onMessageHandler);
    }

    @Override
    public void pushTestMessage(String jsonMessage) {
        AcknowledgementHandlerInternal ackHandler = new AcknowledgementHandlerInternalStub();
        rawMessageHandlers.forEach(handler -> handler.onMessage(jsonMessage, ackHandler));
    }

    private static class AcknowledgementHandlerInternalStub implements AcknowledgementHandlerInternal {

        @Override
        public void autoConfirm() {

        }

        @Override
        public void autoReject() {

        }

        @Override
        public void autoRetry() {

        }

        @Override
        public void setAutoAcknowledgement(boolean b) {

        }

        @Override
        public boolean isAutoAcknowledgement() {
            return false;
        }

        @Override
        public void confirmMessage() {

        }

        @Override
        public void retryMessage() {

        }

        @Override
        public void rejectMessage() {

        }
    }
}
