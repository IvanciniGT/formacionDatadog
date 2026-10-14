package com.course.shop.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    private Integer quantity;
    private BigDecimal total;
    private String status;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Order() {}

    public Order(String customer, Product product, Integer quantity) {
        this.customer = customer;
        this.product = product;
        this.quantity = quantity;
        this.total = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public String getCustomer() { return customer; }
    public Product getProduct() { return product; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
