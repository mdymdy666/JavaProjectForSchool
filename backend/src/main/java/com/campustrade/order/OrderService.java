package com.campustrade.order;

import static com.campustrade.order.OrderDtos.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.product.Product;
import com.campustrade.product.ProductMapper;
import com.campustrade.message.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    public OrderService(OrderMapper orderMapper, ProductMapper productMapper,
            NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderView createOrder(long buyerId, CreateOrderRequest request) {
        Product product = productMapper.selectById(request.productId());
        if (product == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能购买自己发布的商品");
        }
        if (!"APPROVED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "商品当前不可购买");
        }
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setTotalAmount(product.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setVersion(0);
        order.setDeleted(0);
        orderMapper.insert(order);
        if (productMapper.markSold(product.getId(), product.getVersion()) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "商品已被其他用户购买");
        }
        orderMapper.insertLog(order.getId(), null, order.getStatus(), buyerId, "创建订单");
        notificationService.create(order.getSellerId(), "ORDER_STATUS", "收到新的订单",
                "订单 " + order.getOrderNo() + " 等待买家支付");
        return view(order);
    }

    public List<OrderView> list(long userId, String role) {
        LambdaQueryWrapper<TradeOrder> query = new LambdaQueryWrapper<>();
        if ("seller".equalsIgnoreCase(role)) {
            query.eq(TradeOrder::getSellerId, userId);
        } else {
            query.eq(TradeOrder::getBuyerId, userId);
        }
        query.orderByDesc(TradeOrder::getCreatedAt).orderByDesc(TradeOrder::getId);
        return orderMapper.selectList(query).stream().map(this::view).toList();
    }

    @Transactional
    public OrderView pay(long buyerId, long orderId) {
        return transition(orderId, buyerId, true, OrderStatus.PAID, null, "模拟支付成功");
    }

    @Transactional
    public OrderView ship(long sellerId, long orderId, ShipRequest request) {
        return transition(orderId, sellerId, false, OrderStatus.SHIPPED,
                request.logisticsInfo().trim(), "卖家发货");
    }

    @Transactional
    public OrderView confirm(long buyerId, long orderId) {
        return transition(orderId, buyerId, true, OrderStatus.COMPLETED, null, "买家确认收货");
    }

    @Transactional
    public OrderView cancel(long buyerId, long orderId) {
        TradeOrder result = transitionEntity(orderId, buyerId, true, OrderStatus.CANCELED, null, "买家取消订单");
        if (productMapper.restoreApproved(result.getProductId()) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "商品状态恢复失败");
        }
        return view(result);
    }

    private OrderView transition(long id, long operatorId, boolean buyerOperation,
            OrderStatus target, String logistics, String remark) {
        return view(transitionEntity(id, operatorId, buyerOperation, target, logistics, remark));
    }

    private TradeOrder transitionEntity(long id, long operatorId, boolean buyerOperation,
            OrderStatus target, String logistics, String remark) {
        TradeOrder order = requireOrder(id);
        long ownerId = buyerOperation ? order.getBuyerId() : order.getSellerId();
        if (ownerId != operatorId) throw new BusinessException(ErrorCode.FORBIDDEN);
        OrderStatus source = OrderStatus.valueOf(order.getStatus());
        if (!source.canTransitionTo(target)) throw new BusinessException(ErrorCode.INVALID_STATE);
        String nextLogistics = logistics == null ? order.getLogisticsInfo() : logistics;
        if (orderMapper.transition(id, source.name(), target.name(), order.getVersion(), nextLogistics) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "订单状态已变化，请刷新后重试");
        }
        orderMapper.insertLog(id, source.name(), target.name(), operatorId, remark);
        long receiverId = buyerOperation ? order.getSellerId() : order.getBuyerId();
        notificationService.create(receiverId, "ORDER_STATUS", "订单状态更新",
                "订单 " + order.getOrderNo() + " 已变更为 " + target.name());
        order.setStatus(target.name());
        order.setLogisticsInfo(nextLogistics);
        order.setVersion(order.getVersion() + 1);
        return order;
    }

    private TradeOrder requireOrder(long id) {
        TradeOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    private OrderView view(TradeOrder order) {
        return new OrderView(order.getId(), order.getOrderNo(), order.getBuyerId(), order.getSellerId(),
                order.getProductId(), order.getTotalAmount(), order.getStatus(), order.getLogisticsInfo(),
                order.getVersion(), order.getCreatedAt());
    }

    private String generateOrderNo() {
        return "CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
