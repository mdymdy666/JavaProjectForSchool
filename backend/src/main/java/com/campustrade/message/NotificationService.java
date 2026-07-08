package com.campustrade.message;

import static com.campustrade.message.MessageDtos.NotificationView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;

@Service
public class NotificationService {
    private final NotificationMapper mapper;
    public NotificationService(NotificationMapper mapper) { this.mapper = mapper; }

    public void create(long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId); n.setType(type); n.setTitle(title); n.setContent(content); n.setReadStatus("UNREAD");
        mapper.insert(n);
    }

    public List<NotificationView> list(long userId) {
        return mapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt).orderByDesc(Notification::getId))
                .stream().map(this::view).toList();
    }

    @Transactional
    public NotificationView read(long userId, long id) {
        Notification n = mapper.selectById(id);
        if (n == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "通知不存在");
        if (!n.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        n.setReadStatus("READ"); mapper.updateById(n); return view(n);
    }

    private NotificationView view(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getContent(), n.getReadStatus(), n.getCreatedAt());
    }
}
