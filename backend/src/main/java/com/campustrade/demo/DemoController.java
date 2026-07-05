package com.campustrade.demo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.common.ApiResponse;
import com.campustrade.demo.DemoService.AuditRequest;
import com.campustrade.demo.DemoService.CreateOrderRequest;
import com.campustrade.demo.DemoService.Dashboard;
import com.campustrade.demo.DemoService.DemoLogin;
import com.campustrade.demo.DemoService.DemoLoginRequest;
import com.campustrade.demo.DemoService.MessageItem;
import com.campustrade.demo.DemoService.OrderItem;
import com.campustrade.demo.DemoService.ProductItem;
import com.campustrade.demo.DemoService.PublishProductRequest;
import com.campustrade.demo.DemoService.SendMessageRequest;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<DemoLogin> login(@RequestBody DemoLoginRequest request) {
        return ApiResponse.success(demoService.login(request));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        return ApiResponse.success(demoService.dashboard());
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductItem>> products() {
        return ApiResponse.success(demoService.products());
    }

    @PostMapping("/products")
    public ApiResponse<ProductItem> publishProduct(@RequestBody PublishProductRequest request) {
        return ApiResponse.success(demoService.publish(request));
    }

    @PostMapping("/products/{productId}/audit")
    public ApiResponse<ProductItem> auditProduct(@PathVariable long productId, @RequestBody AuditRequest request) {
        return ApiResponse.success(demoService.auditProduct(productId, request));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderItem>> orders() {
        return ApiResponse.success(demoService.orders());
    }

    @PostMapping("/orders")
    public ApiResponse<OrderItem> createOrder(@RequestBody CreateOrderRequest request) {
        return ApiResponse.success(demoService.createOrder(request));
    }

    @PostMapping("/orders/{orderId}/{action}")
    public ApiResponse<OrderItem> advanceOrder(@PathVariable long orderId, @PathVariable String action) {
        return ApiResponse.success(demoService.advanceOrder(orderId, action));
    }

    @GetMapping("/messages")
    public ApiResponse<List<MessageItem>> messages() {
        return ApiResponse.success(demoService.messages());
    }

    @PostMapping("/messages")
    public ApiResponse<MessageItem> sendMessage(@RequestBody SendMessageRequest request) {
        return ApiResponse.success(demoService.sendMessage(request));
    }
}
