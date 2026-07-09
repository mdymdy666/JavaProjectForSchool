from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path("campus_trade_package_diagram.png")
SCALE = 3
W, H = 3300 * SCALE, 2100 * SCALE


def font(size, bold=False):
    size = int(size * SCALE)
    choices = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\calibrib.ttf" if bold else r"C:\Windows\Fonts\calibri.ttf",
    ]
    for item in choices:
        if Path(item).exists():
            return ImageFont.truetype(item, size)
    return ImageFont.load_default()


TITLE = font(24, True)
PKG = font(18, True)
TEXT = font(15)
SMALL = font(13)

BG = "#ffffff"
LAYER_FILL = "#f8fafc"
LAYER_BORDER = "#94a3b8"
FILL_FRONT = "#dbeafe"
FILL_BACK = "#dcfce7"
FILL_DATA = "#fef3c7"
FILL_CROSS = "#f3e8ff"
FILL_EXTERNAL = "#fee2e2"
TXT = "#1f2937"
MUTED = "#475569"
LINE = "#64748b"


def S(v):
    return int(v * SCALE)


img = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)


def layer(x, y, w, h, title):
    draw.rounded_rectangle((S(x), S(y), S(x + w), S(y + h)), radius=S(18), fill=LAYER_FILL, outline=LAYER_BORDER, width=S(2))
    draw.text((S(x + 24), S(y + 16)), title, font=TITLE, fill=TXT)


def package(x, y, w, h, name, items, fill):
    x1, y1, x2, y2 = S(x), S(y), S(x + w), S(y + h)
    tab_w, tab_h = S(min(170, w * 0.45)), S(28)
    draw.rounded_rectangle((x1, y1 + tab_h, x2, y2), radius=S(10), fill="#ffffff", outline=LAYER_BORDER, width=S(2))
    draw.rounded_rectangle((x1, y1, x1 + tab_w, y1 + tab_h + S(8)), radius=S(8), fill=fill, outline=LAYER_BORDER, width=S(2))
    draw.rectangle((x1, y1 + tab_h, x1 + tab_w, y1 + tab_h + S(8)), fill=fill)
    draw.text((x1 + S(12), y1 + S(4)), name, font=PKG, fill=TXT)
    yy = y1 + tab_h + S(18)
    for item in items:
        draw.text((x1 + S(18), yy), item, font=TEXT, fill=MUTED)
        yy += S(27)


def center(x, y, w, h):
    return S(x + w / 2), S(y + h / 2)


def edge(x1, y1, x2, y2, label="", bend=None):
    if bend is None:
        points = [(S(x1), S(y1)), (S(x2), S(y2))]
    else:
        points = [(S(x1), S(y1)), (S(bend[0]), S(y1)), (S(bend[0]), S(y2)), (S(x2), S(y2))]
    draw.line(points, fill=LINE, width=S(2), joint="curve")
    if label:
        lx, ly = (S((x1 + x2) / 2), S((y1 + y2) / 2))
        tw = draw.textlength(label, font=SMALL)
        draw.rounded_rectangle((lx - tw / 2 - S(6), ly - S(12), lx + tw / 2 + S(6), ly + S(12)), radius=S(5), fill=BG)
        draw.text((lx - tw / 2, ly - S(10)), label, font=SMALL, fill=LINE)


# Layer containers
layer(70, 70, 3160, 470, "前端包 frontend/src")
layer(70, 610, 3160, 980, "后端包 com.campustrade")
layer(70, 1660, 3160, 300, "数据与外部资源")

# Frontend packages
package(130, 155, 360, 270, "views", ["HomeView", "ProductListView", "ProductDetailView", "OrderCenterView", "AdminDashboardView"], FILL_FRONT)
package(570, 155, 360, 270, "components", ["AppHeader", "ProductCard", "StatusTag", "chat/*"], FILL_FRONT)
package(1010, 155, 360, 270, "stores", ["auth", "cart", "notification"], FILL_FRONT)
package(1450, 155, 360, 270, "router", ["index.ts", "路由表", "登录/管理员守卫"], FILL_FRONT)
package(1890, 155, 390, 270, "api", ["http.ts", "auth/product/order", "message/admin/upload"], FILL_FRONT)
package(2370, 155, 360, 270, "types/features", ["domain.ts", "features/chat", "类型与前端业务工具"], FILL_FRONT)

# Backend packages
package(130, 710, 360, 240, "config", ["SecurityConfig", "MybatisPlusConfig", "UploadResourceConfig"], FILL_CROSS)
package(570, 710, 360, 240, "security", ["JwtService", "JwtAuthenticationFilter", "SecurityUser"], FILL_CROSS)
package(1010, 710, 360, 240, "common", ["ApiResponse", "BusinessException", "GlobalExceptionHandler"], FILL_CROSS)
package(1450, 710, 360, 240, "cache", ["RedisSupport", "CacheNames"], FILL_CROSS)

package(130, 1040, 360, 300, "auth", ["AuthController", "AuthService", "AuthMapper", "UserAccount"], FILL_BACK)
package(570, 1040, 360, 300, "user", ["UserController", "用户资料维护"], FILL_BACK)
package(1010, 1040, 390, 300, "product", ["ProductController", "ProductService", "ProductMapper", "Favorite/ProductImage"], FILL_BACK)
package(1480, 1040, 360, 300, "order", ["OrderController", "OrderService", "OrderMapper", "TradeOrder"], FILL_BACK)
package(1920, 1040, 390, 300, "message", ["MessageController", "MessageService", "NotificationService", "MessageMapper"], FILL_BACK)
package(2390, 1040, 390, 300, "admin", ["AdminController", "AdminService", "AnnouncementController", "后台审核与统计"], FILL_BACK)

package(130, 1390, 360, 240, "api", ["HealthController", "UploadController"], FILL_BACK)
package(570, 1390, 390, 240, "extension", ["RecommendationService", "ContentReviewService", "ReportService"], FILL_BACK)
package(1010, 1390, 360, 240, "demo", ["DemoController", "DemoService"], FILL_BACK)

# Data/external packages
package(190, 1740, 540, 160, "MySQL", ["users/products/orders/messages", "favorites/reports/audit_logs"], FILL_DATA)
package(880, 1740, 470, 160, "Redis", ["验证码", "登录限流", "JWT 黑名单", "商品热度"], FILL_DATA)
package(1500, 1740, 470, 160, "uploads", ["商品图片", "用户头像", "静态资源映射"], FILL_EXTERNAL)
package(2120, 1740, 470, 160, "Knife4j/OpenAPI", ["接口文档", "调试入口"], FILL_EXTERNAL)

# Main dependencies
edge(2085, 425, 2085, 1040, "REST API", bend=(2085, 760))
edge(2085, 425, 1190, 1040, "", bend=(2085, 760))
edge(2085, 425, 1660, 1040, "", bend=(2085, 760))
edge(2085, 425, 2115, 1040, "", bend=(2085, 760))
edge(2085, 425, 2585, 1040, "", bend=(2085, 760))

edge(1300, 1340, 460, 1740, "Mapper/JDBC", bend=(1300, 1620))
edge(1660, 1340, 460, 1740, "", bend=(1660, 1620))
edge(2115, 1340, 460, 1740, "", bend=(2115, 1620))
edge(765, 1630, 460, 1740, "", bend=(765, 1660))

edge(1630, 950, 1115, 1740, "缓存/限流", bend=(1630, 1610))
edge(750, 950, 1115, 1740, "鉴权黑名单", bend=(750, 1610))
edge(315, 1390, 1735, 1740, "文件上传", bend=(315, 1660))
edge(315, 710, 2355, 1740, "接口文档", bend=(315, 1640))

edge(750, 950, 310, 1040, "认证上下文")
edge(1190, 950, 310, 1040, "统一响应/异常")
edge(750, 950, 1260, 1040, "")
edge(750, 950, 1660, 1040, "")
edge(750, 950, 2115, 1040, "")
edge(750, 950, 2585, 1040, "")

# Notes as tiny footer text, not a legend box.
draw.text((S(90), S(2010)), "依赖方向：前端 api 调用后端 Controller；业务包通过 Service/Mapper 访问 MySQL；security/common/cache/config 为横切基础包。", font=SMALL, fill="#64748b")

img.save(OUT, dpi=(600, 600))
print(OUT.resolve())
