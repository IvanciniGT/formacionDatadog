package com.course.shop.web;

import com.course.shop.domain.Order;
import com.course.shop.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    public record NewOrder(String customer, Long productId, Integer quantity) {}
    public record OrderView(Long id, String customer, String product, Integer quantity, Double total, String status) {
        static OrderView of(Order o) {
            return new OrderView(o.getId(), o.getCustomer(), o.getProduct().getName(), o.getQuantity(),
                    o.getTotal().doubleValue(), o.getStatus());
        }
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView create(@RequestBody NewOrder body) {
        return OrderView.of(service.place(body.customer(), body.productId(), body.quantity() == null ? 1 : body.quantity()));
    }

    @GetMapping("/orders")
    public List<OrderView> latest() {
        return service.latest().stream().map(OrderView::of).toList();
    }

    // ?fast=true uses the GROUP BY version; the default stays slow on purpose until lab09 flips it
    @GetMapping("/report")
    public Map<String, Long> report(@RequestParam(defaultValue = "false") boolean fast) {
        return fast ? service.ordersPerProductFast() : service.ordersPerProduct();
    }
}
