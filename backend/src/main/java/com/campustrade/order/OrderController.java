package com.campustrade.order;

import static com.campustrade.order.OrderDtos.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

import com.campustrade.common.ApiResponse;
import com.campustrade.security.SecurityUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping
    public ApiResponse<List<OrderView>> list(
            @RequestParam(defaultValue = "buyer") String role,
            Authentication auth) {
        return ApiResponse.success(orderService.list(userId(auth), role));
    }

    @PostMapping
    public ApiResponse<OrderView> create(@Valid @RequestBody CreateOrderRequest request, Authentication auth) {
        return ApiResponse.success(orderService.createOrder(userId(auth), request));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderView> pay(@PathVariable long id, Authentication auth) {
        return ApiResponse.success(orderService.pay(userId(auth), id));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<OrderView> ship(@PathVariable long id, @Valid @RequestBody ShipRequest request, Authentication auth) {
        return ApiResponse.success(orderService.ship(userId(auth), id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderView> confirm(@PathVariable long id, Authentication auth) {
        return ApiResponse.success(orderService.confirm(userId(auth), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderView> cancel(@PathVariable long id, Authentication auth) {
        return ApiResponse.success(orderService.cancel(userId(auth), id));
    }

    private long userId(Authentication auth) {
        return ((SecurityUser) auth.getPrincipal()).userId();
    }
}
