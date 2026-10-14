package com.course.shop.service;

import datadog.trace.api.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Simulates a third-party payment provider: variable latency and a failure
 * rate of ~30 %. It is the "flaky dependency" of the demo: the thing you want
 * to see as its own span in the flame graph and as its own error in Error
 * Tracking.
 */
@Service
public class PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PaymentGateway.class);
    private final Random random = new Random();
    private final Metrics metrics;

    public PaymentGateway(Metrics metrics) {
        this.metrics = metrics;
    }

    // @Trace creates a custom span: the auto-instrumentation only sees HTTP,
    // JDBC, etc. Business steps need to be declared explicitly.
    @Trace(operationName = "payment.charge", resourceName = "PaymentGateway.charge")
    public String charge(String customer, double amount) {
        long start = System.currentTimeMillis();
        try {
            Thread.sleep(100 + random.nextInt(700));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean ok = random.nextInt(100) >= 30;
        long elapsed = System.currentTimeMillis() - start;
        metrics.paymentDuration(elapsed, ok);
        if (!ok) {
            log.error("Payment rejected customer={} amount={} elapsed_ms={} reason=GATEWAY_TIMEOUT", customer, amount, elapsed);
            throw new PaymentException("Payment gateway timeout for customer " + customer);
        }
        log.info("Payment accepted customer={} amount={} elapsed_ms={}", customer, amount, elapsed);
        return "PAY-" + Long.toHexString(System.nanoTime());
    }

    public static class PaymentException extends RuntimeException {
        public PaymentException(String message) { super(message); }
    }
}
