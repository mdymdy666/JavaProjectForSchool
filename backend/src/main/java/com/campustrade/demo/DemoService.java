package com.campustrade.demo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class DemoService {

    private final AtomicLong productIds = new AtomicLong(100);
    private final AtomicLong orderIds = new AtomicLong(300);
    private final AtomicLong messageIds = new AtomicLong(500);
    private final List<ProductItem> products = new CopyOnWriteArrayList<>();
    private final List<OrderItem> orders = new CopyOnWriteArrayList<>();
    private final List<MessageItem> messages = new CopyOnWriteArrayList<>();

    public DemoService() {
        products.add(new ProductItem(1L, "九成新机械键盘", "数码配件", "计科小李", "九成新",
                new BigDecimal("129.00"), "APPROVED", 238, "Cherry 轴体，送拔键器，适合编程和游戏。"));
        products.add(new ProductItem(2L, "考研数学复习全书", "图书教材", "经管小周", "轻微笔记",
                new BigDecimal("35.00"), "APPROVED", 156, "2026 版，重点章节做了标注。"));
        products.add(new ProductItem(3L, "宿舍折叠桌", "生活用品", "软件小陈", "八成新",
                new BigDecimal("48.00"), "PENDING", 64, "可放笔记本电脑，宿舍自提。"));
        products.add(new ProductItem(4L, "校园卡保护套", "生活用品", "美院小林", "全新",
                new BigDecimal("9.90"), "APPROVED", 81, "透明硬壳，附挂绳。"));

        orders.add(new OrderItem(301L, "CT20260704001", 1L, "九成新机械键盘", "软件小陈", "计科小李",
                new BigDecimal("129.00"), "PAID", "等待卖家发货", LocalDateTime.now().minusHours(3)));
        orders.add(new OrderItem(302L, "CT20260704002", 2L, "考研数学复习全书", "计科小李", "经管小周",
                new BigDecimal("35.00"), "COMPLETED", "已完成交易", LocalDateTime.now().minusDays(1)));

        messages.add(new MessageItem(501L, "软件小陈", "计科小李", "九成新机械键盘", "键盘今晚可以在图书馆门口交易吗？",
                "UNREAD", LocalDateTime.now().minusMinutes(28)));
        messages.add(new MessageItem(502L, "管理员", "全体用户", "系统公告", "平台演示数据已初始化，可直接按流程答辩。",
                "READ", LocalDateTime.now().minusHours(2)));
    }

    public Dashboard dashboard() {
        long approvedProducts = products.stream().filter(product -> "APPROVED".equals(product.status())).count();
        long pendingProducts = products.stream().filter(product -> "PENDING".equals(product.status())).count();
        BigDecimal turnover = orders.stream()
                .filter(order -> "COMPLETED".equals(order.status()))
                .map(OrderItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> categoryStats = Map.of(
                "数码配件", countCategory("数码配件"),
                "图书教材", countCategory("图书教材"),
                "生活用品", countCategory("生活用品"));

        return new Dashboard(
                List.of(
                        new Metric("注册用户", "128", "+12 本周"),
                        new Metric("在售商品", String.valueOf(approvedProducts), pendingProducts + " 件待审核"),
                        new Metric("订单总数", String.valueOf(orders.size()), "含模拟支付"),
                        new Metric("成交金额", "￥" + turnover, "答辩可演示闭环")),
                categoryStats);
    }

    public List<ProductItem> products() {
        return products.stream()
                .sorted(Comparator.comparing(ProductItem::status).thenComparing(ProductItem::id))
                .toList();
    }

    public ProductItem publish(PublishProductRequest request) {
        ProductItem product = new ProductItem(
                productIds.incrementAndGet(),
                request.title(),
                request.category(),
                request.seller(),
                request.itemCondition(),
                request.price(),
                "PENDING",
                0,
                request.description());
        products.add(product);
        return product;
    }

    public ProductItem auditProduct(long productId, AuditRequest request) {
        ProductItem current = findProduct(productId);
        ProductItem updated = new ProductItem(
                current.id(),
                current.title(),
                current.category(),
                current.seller(),
                current.itemCondition(),
                current.price(),
                request.approved() ? "APPROVED" : "REJECTED",
                current.views(),
                current.description());
        replaceProduct(updated);
        return updated;
    }

    public List<OrderItem> orders() {
        return orders.stream()
                .sorted(Comparator.comparing(OrderItem::createdAt).reversed())
                .toList();
    }

    public OrderItem createOrder(CreateOrderRequest request) {
        ProductItem product = findProduct(request.productId());
        OrderItem order = new OrderItem(
                orderIds.incrementAndGet(),
                "CT" + System.currentTimeMillis(),
                product.id(),
                product.title(),
                request.buyer(),
                product.seller(),
                product.price(),
                "PENDING_PAYMENT",
                "订单已创建，等待模拟支付",
                LocalDateTime.now());
        orders.add(order);
        return order;
    }

    public OrderItem advanceOrder(long orderId, String action) {
        OrderItem current = findOrder(orderId);
        String status = switch (action) {
            case "pay" -> "PAID";
            case "ship" -> "SHIPPED";
            case "complete" -> "COMPLETED";
            default -> current.status();
        };
        String remark = switch (status) {
            case "PAID" -> "买家已完成模拟支付";
            case "SHIPPED" -> "卖家已填写校内交付信息";
            case "COMPLETED" -> "买家确认收货，交易闭环完成";
            default -> current.remark();
        };
        OrderItem updated = new OrderItem(current.id(), current.orderNo(), current.productId(), current.productTitle(),
                current.buyer(), current.seller(), current.amount(), status, remark, current.createdAt());
        replaceOrder(updated);
        return updated;
    }

    public List<MessageItem> messages() {
        return messages.stream()
                .sorted(Comparator.comparing(MessageItem::createdAt).reversed())
                .toList();
    }

    public MessageItem sendMessage(SendMessageRequest request) {
        MessageItem message = new MessageItem(
                messageIds.incrementAndGet(),
                request.sender(),
                request.receiver(),
                request.productTitle(),
                request.content(),
                "UNREAD",
                LocalDateTime.now());
        messages.add(message);
        return message;
    }

    public DemoLogin login(DemoLoginRequest request) {
        String role = "admin".equalsIgnoreCase(request.username()) ? "ADMIN" : "USER";
        String nickname = "ADMIN".equals(role) ? "管理员" : "演示用户";
        return new DemoLogin(nickname, role, "demo-token-" + role.toLowerCase());
    }

    private long countCategory(String category) {
        return products.stream().filter(product -> category.equals(product.category())).count();
    }

    private ProductItem findProduct(long id) {
        return products.stream()
                .filter(product -> product.id() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    }

    private OrderItem findOrder(long id) {
        return orders.stream()
                .filter(order -> order.id() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    private void replaceProduct(ProductItem updated) {
        products.removeIf(product -> product.id().equals(updated.id()));
        products.add(updated);
    }

    private void replaceOrder(OrderItem updated) {
        orders.removeIf(order -> order.id().equals(updated.id()));
        orders.add(updated);
    }

    public record Dashboard(List<Metric> metrics, Map<String, Long> categoryStats) {
    }

    public record Metric(String label, String value, String hint) {
    }

    public record ProductItem(Long id, String title, String category, String seller, String itemCondition,
            BigDecimal price, String status, int views, String description) {
    }

    public record OrderItem(Long id, String orderNo, Long productId, String productTitle, String buyer, String seller,
            BigDecimal amount, String status, String remark, LocalDateTime createdAt) {
    }

    public record MessageItem(Long id, String sender, String receiver, String productTitle, String content,
            String status, LocalDateTime createdAt) {
    }

    public record PublishProductRequest(String title, String category, String seller, String itemCondition,
            BigDecimal price, String description) {
    }

    public record AuditRequest(boolean approved, String reason) {
    }

    public record CreateOrderRequest(Long productId, String buyer) {
    }

    public record SendMessageRequest(String sender, String receiver, String productTitle, String content) {
    }

    public record DemoLoginRequest(String username, String password) {
    }

    public record DemoLogin(String nickname, String role, String token) {
    }
}
