package com.course.shop.web;

import com.course.shop.service.Metrics;
import com.course.shop.service.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

/**
 * Endpoints that exist only to make things visible in Datadog: latency,
 * errors, a flaky dependency, a custom event. The load generator calls them.
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private final Random random = new Random();
    private final PaymentGateway payments;
    private final Metrics metrics;

    public DemoController(PaymentGateway payments, Metrics metrics) {
        this.payments = payments;
        this.metrics = metrics;
    }

    @GetMapping("/slow")
    public Map<String, Object> slow() throws InterruptedException {
        long delay = 500 + random.nextInt(2500);
        log.info("Slow endpoint sleeping delay_ms={}", delay);
        Thread.sleep(delay);
        return Map.of("delayMs", delay);
    }

    @GetMapping("/error")
    public Map<String, Object> error() {
        log.error("Deliberate failure in /api/error");
        throw new IllegalStateException("Boom: this endpoint always fails");
    }

    @GetMapping("/payment")
    public Map<String, Object> payment() {
        String ref = payments.charge("anonymous", 9.99);
        return Map.of("reference", ref);
    }

    @PostMapping("/deploy")
    public Map<String, Object> deploy(@RequestParam(defaultValue = "1.0.0") String version) {
        metrics.event("shop-api deployed", "Version " + version + " deployed by the demo endpoint");
        log.info("Deployment event sent version={}", version);
        return Map.of("version", version);
    }
}
