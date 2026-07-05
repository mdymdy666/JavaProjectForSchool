package com.campustrade.admin;

import static com.campustrade.admin.AdminDtos.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class AdminService {
    private final AdminMapper adminMapper;
    private final AnnouncementMapper announcementMapper;
    public AdminService(AdminMapper adminMapper, AnnouncementMapper announcementMapper) {
        this.adminMapper = adminMapper; this.announcementMapper = announcementMapper;
    }
    public DashboardView dashboard() {
        return new DashboardView(adminMapper.userCount(), adminMapper.productCount(),
                adminMapper.orderCount(), adminMapper.turnover(), adminMapper.categoryStats());
    }
    @Transactional
    public AnnouncementView create(AnnouncementRequest request) {
        Announcement a = new Announcement();
        a.setTitle(request.title().trim()); a.setContent(request.content().trim());
        a.setPublished(request.published() ? 1 : 0); announcementMapper.insert(a); return view(a);
    }
    @Transactional public void delete(long id) { announcementMapper.deleteById(id); }
    public List<AnnouncementView> published() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getPublished, 1).orderByDesc(Announcement::getCreatedAt))
                .stream().map(this::view).toList();
    }
    private AnnouncementView view(Announcement a) {
        return new AnnouncementView(a.getId(), a.getTitle(), a.getContent(),
                Integer.valueOf(1).equals(a.getPublished()), a.getCreatedAt());
    }
}
