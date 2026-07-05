package com.campustrade.message;

import static com.campustrade.message.MessageDtos.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.product.Product;
import com.campustrade.product.ProductMapper;

@Service
public class MessageService {
    private final MessageMapper messageMapper;
    private final ProductMapper productMapper;
    private final AuthMapper authMapper;

    public MessageService(MessageMapper messageMapper, ProductMapper productMapper, AuthMapper authMapper) {
        this.messageMapper = messageMapper;
        this.productMapper = productMapper;
        this.authMapper = authMapper;
    }

    @Transactional
    public MessageView send(long senderId, SendRequest request) {
        if (senderId == request.receiverId()) throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己留言");
        Product product = productMapper.selectById(request.productId());
        if (product == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        boolean contactingSeller = product.getSellerId().equals(request.receiverId());
        boolean tradeParticipants = messageMapper.areTradeParticipants(
                request.productId(), senderId, request.receiverId());
        if (!contactingSeller && !tradeParticipants) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能联系该商品卖家");
        }
        SiteMessage message = new SiteMessage();
        message.setSenderId(senderId);
        message.setReceiverId(request.receiverId());
        message.setProductId(request.productId());
        message.setContent(request.content().trim());
        message.setStatus("UNREAD");
        messageMapper.insert(message);
        return view(message);
    }

    public List<MessageView> list(long userId) {
        return messageMapper.selectList(new LambdaQueryWrapper<SiteMessage>()
                        .and(q -> q.eq(SiteMessage::getSenderId, userId).or().eq(SiteMessage::getReceiverId, userId))
                        .orderByDesc(SiteMessage::getCreatedAt).orderByDesc(SiteMessage::getId))
                .stream().map(this::view).toList();
    }

    @Transactional
    public MessageView read(long receiverId, long messageId) {
        SiteMessage message = messageMapper.selectById(messageId);
        if (message == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不存在");
        if (!message.getReceiverId().equals(receiverId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!"READ".equals(message.getStatus())) {
            message.setStatus("READ");
            messageMapper.updateById(message);
        }
        return view(message);
    }

    private MessageView view(SiteMessage message) {
        UserAccount sender = authMapper.selectById(message.getSenderId());
        return new MessageView(message.getId(), message.getSenderId(),
                sender == null ? "未知用户" : sender.getNickname(), message.getReceiverId(),
                message.getProductId(), message.getContent(), message.getStatus(), message.getCreatedAt());
    }
}
