package com.course.shop.service;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over the DogStatsD client. All custom metrics of the app go
 * through here so the metric names live in one place and are easy to show in
 * class. Metric names follow the convention <app>.<thing>.<what>.
 */
@Component
public class Metrics {

    private final StatsDClient client;

    public Metrics() {
        // DD_AGENT_HOST / DD_DOGSTATSD_PORT are read by the client automatically;
        // we only set the prefix and the constant tags. Service/env/version
        // come from DD_SERVICE, DD_ENV and DD_VERSION (Unified Service Tagging).
        this.client = new NonBlockingStatsDClientBuilder()
                .prefix("shop")
                .hostname(System.getenv().getOrDefault("DD_AGENT_HOST", "localhost"))
                .port(Integer.parseInt(System.getenv().getOrDefault("DD_DOGSTATSD_PORT", "8125")))
                .build();
    }

    public void orderCreated(String category, String status) {
        client.incrementCounter("orders.created", "category:" + category, "status:" + status);
    }

    public void orderValue(double euros, String category) {
        client.distribution("orders.value", euros, "category:" + category);
    }

    public void paymentDuration(long millis, boolean ok) {
        client.histogram("payment.duration", millis, "status:" + (ok ? "ok" : "failed"));
    }

    public void stockLevel(String productName, int stock) {
        client.gauge("stock.units", stock, "product:" + productName);
    }

    public void event(String title, String text) {
        client.recordEvent(com.timgroup.statsd.Event.builder()
                .withTitle(title).withText(text)
                .withAlertType(com.timgroup.statsd.Event.AlertType.INFO).build());
    }
}
