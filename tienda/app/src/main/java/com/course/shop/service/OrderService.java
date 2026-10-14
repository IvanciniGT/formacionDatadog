package com.course.shop.service;

import com.course.shop.domain.Order;
import com.course.shop.domain.Product;
import com.course.shop.repository.OrderRepository;
import com.course.shop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.opentelemetry.api.trace.Span;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProductRepository products;
    private final PaymentGateway payments;
    private final Metrics metrics;

    public OrderService(OrderRepository orders, ProductRepository products, PaymentGateway payments, Metrics metrics) {
        this.orders = orders;
        this.products = products;
        this.payments = payments;
        this.metrics = metrics;
    }

    @Transactional
    public Order place(String customer, Long productId, int quantity) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product " + productId + " does not exist"));
        if (product.getStock() < quantity) {
            log.warn("Out of stock product={} requested={} available={}", product.getName(), quantity, product.getStock());
            metrics.orderCreated(product.getCategory(), "out_of_stock");
            throw new IllegalStateException("Not enough stock for " + product.getName());
        }
        // Business context on the active span (the servlet/handler span created by the tracer):
        // low-cardinality tags you can filter and group traces by. usr.name is a standard attribute.
        Span span = Span.current();
        span.setAttribute("order.category", product.getCategory());
        span.setAttribute("order.quantity", quantity);
        span.setAttribute("usr.name", customer);
        Order order = new Order(customer, product, quantity);
        try {
            payments.charge(customer, order.getTotal().doubleValue());
            order.setStatus("PAID");
            product.setStock(product.getStock() - quantity);
            metrics.stockLevel(product.getName(), product.getStock());
        } catch (PaymentGateway.PaymentException e) {
            order.setStatus("PAYMENT_FAILED");
        }
        orders.save(order);
        metrics.orderCreated(product.getCategory(), order.getStatus());
        metrics.orderValue(order.getTotal().doubleValue(), product.getCategory());
        log.info("Order created id={} customer={} product={} quantity={} total={} status={}",
                order.getId(), customer, product.getName(), quantity, order.getTotal(), order.getStatus());
        if ("PAYMENT_FAILED".equals(order.getStatus())) {
            throw new PaymentGateway.PaymentException("Order " + order.getId() + " could not be paid");
        }
        return order;
    }

    public List<Order> latest() {
        return orders.findLatestWithProduct(PageRequest.of(0, 20));
    }

    /**
     * Deliberately bad: one query to list the products and then one COUNT per
     * product. In class this is the N+1 that DBM and the APM flame graph make
     * obvious. The fix (a GROUP BY) is the exercise.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> ordersPerProduct() {
        Map<String, Long> report = new LinkedHashMap<>();
        for (Product p : products.findAll()) {
            report.put(p.getName(), orders.countByProductId(p.getId()));
        }
        return report;
    }

    /** The fix: one GROUP BY query. Compare both in APM and DBM (lab09). */
    @Transactional(readOnly = true)
    public Map<String, Long> ordersPerProductFast() {
        Map<String, Long> report = new LinkedHashMap<>();
        for (Object[] row : orders.countByProduct()) {
            report.put((String) row[0], (Long) row[1]);
        }
        return report;
    }
}
