package fixture.retry;

/** Starting code for the approved disposable delivery fixture; not production FDI behavior. */
final class RetryPolicy {
    @FunctionalInterface
    interface Transport {
        String call() throws TransportFailure;
    }

    static final class TransportFailure extends Exception {
        private final String category;

        TransportFailure(String category) {
            super(category);
            this.category = category;
        }

        String category() { return category; }
    }

    /** Delivery task: implement the frozen retry acceptance criteria in this method only. */
    static String execute(boolean readOnly, boolean idempotencyEstablished, int maximumAttempts,
            Transport transport) throws TransportFailure {
        return transport.call();
    }
}
