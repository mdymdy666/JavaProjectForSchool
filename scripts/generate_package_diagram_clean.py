from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path("campus_trade_package_diagram_clean.png")
SCALE = 3
W, H = 3000 * SCALE, 1900 * SCALE


def f(size, bold=False):
    size = int(size * SCALE)
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\calibrib.ttf" if bold else r"C:\Windows\Fonts\calibri.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


FONT_TITLE = f(25, True)
FONT_SUB = f(17)
FONT_PKG = f(18, True)
FONT_TEXT = f(14)
FONT_LABEL = f(14, True)

BG = "#ffffff"
LAYER_BG = "#f8fafc"
LAYER_BORDER = "#94a3b8"
TEXT = "#1f2937"
MUTED = "#475569"
LINE = "#52647a"
BLUE = "#dbeafe"
GREEN = "#dcfce7"
PURPLE = "#f3e8ff"
YELLOW = "#fef3c7"
RED = "#fee2e2"


def S(n):
    return int(n * SCALE)


img = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)


def layer(x, y, w, h, title, subtitle=""):
    draw.rounded_rectangle((S(x), S(y), S(x + w), S(y + h)), radius=S(18), fill=LAYER_BG, outline=LAYER_BORDER, width=S(2))
    draw.text((S(x + 22), S(y + 16)), title, font=FONT_TITLE, fill=TEXT)
    if subtitle:
        draw.text((S(x + 22), S(y + 52)), subtitle, font=FONT_SUB, fill=MUTED)


def package(x, y, w, h, title, lines, color):
    x1, y1, x2, y2 = S(x), S(y), S(x + w), S(y + h)
    tab_h = S(30)
    tab_w = S(min(190, max(120, len(title) * 13)))
    draw.rounded_rectangle((x1, y1 + tab_h, x2, y2), radius=S(10), fill="#ffffff", outline=LAYER_BORDER, width=S(2))
    draw.rounded_rectangle((x1, y1, x1 + tab_w, y1 + tab_h + S(8)), radius=S(8), fill=color, outline=LAYER_BORDER, width=S(2))
    draw.rectangle((x1, y1 + tab_h, x1 + tab_w, y1 + tab_h + S(8)), fill=color)
    draw.text((x1 + S(12), y1 + S(4)), title, font=FONT_PKG, fill=TEXT)
    yy = y1 + tab_h + S(20)
    for line in lines:
        draw.text((x1 + S(18), yy), line, font=FONT_TEXT, fill=MUTED)
        yy += S(25)


def arrow(x1, y1, x2, y2, label):
    draw.line((S(x1), S(y1), S(x2), S(y2)), fill=LINE, width=S(3))
    dx = 1 if x2 >= x1 else -1
    dy = 1 if y2 >= y1 else -1
    if abs(y2 - y1) >= abs(x2 - x1):
        points = [(S(x2), S(y2)), (S(x2 - 10), S(y2 - 18 * dy)), (S(x2 + 10), S(y2 - 18 * dy))]
    else:
        points = [(S(x2), S(y2)), (S(x2 - 18 * dx), S(y2 - 10)), (S(x2 - 18 * dx), S(y2 + 10))]
    draw.polygon(points, fill=LINE)
    tw = draw.textlength(label, font=FONT_LABEL)
    mx, my = S((x1 + x2) / 2), S((y1 + y2) / 2)
    draw.rounded_rectangle((mx - tw / 2 - S(8), my - S(16), mx + tw / 2 + S(8), my + S(16)), radius=S(6), fill=BG)
    draw.text((mx - tw / 2, my - S(11)), label, font=FONT_LABEL, fill=LINE)


# Outer layers
layer(70, 70, 2860, 360, "前端包 frontend/src", "负责页面展示、组件复用、状态管理、路由控制和接口调用")
layer(70, 520, 2860, 900, "后端包 com.campustrade", "按业务领域划分包，基础包提供安全、缓存、配置和统一异常处理能力")
layer(70, 1510, 2860, 300, "数据与外部资源", "后端通过 Mapper/JDBC、Redis 客户端和静态资源映射访问底层资源")

# Frontend
package(130, 165, 360, 185, "views", ["页面视图", "首页/商品/订单/消息", "后台管理页面"], BLUE)
package(545, 165, 360, 185, "components", ["公共组件", "ProductCard", "AppHeader / chat"], BLUE)
package(960, 165, 360, 185, "stores", ["Pinia 状态", "auth / cart", "notification"], BLUE)
package(1375, 165, 360, 185, "router", ["路由表", "登录守卫", "管理员守卫"], BLUE)
package(1790, 165, 390, 185, "api", ["Axios 封装", "auth/product/order", "message/admin/upload"], BLUE)
package(2235, 165, 360, 185, "types/features", ["类型定义", "domain.ts", "chat 业务工具"], BLUE)

# Backend infra and business groups
package(130, 630, 420, 220, "基础支撑包", ["config：安全、MyBatis、上传资源配置", "common：统一响应、业务异常、全局异常", "cache：RedisSupport、缓存键定义", "security：JWT 解析与认证过滤"], PURPLE)

package(650, 630, 470, 300, "认证与用户包", ["auth：注册、登录、找回密码、JWT 签发", "user：用户资料维护", "security：登录态识别与权限上下文"], GREEN)
package(1200, 630, 500, 300, "商品业务包", ["product：商品发布、审核、搜索、收藏", "extension：推荐算法、内容审核、举报处理", "api：上传接口与健康检查"], GREEN)
package(1780, 630, 470, 300, "交易沟通包", ["order：下单、支付、发货、收货、取消", "message：站内留言", "notification：系统通知"], GREEN)
package(2330, 630, 470, 300, "后台管理包", ["admin：后台统计与管理", "announcement：公告管理", "audit_logs：审核记录"], GREEN)

package(650, 1040, 470, 220, "Controller 层", ["接收前端请求", "参数校验", "调用业务 Service"], GREEN)
package(1200, 1040, 500, 220, "Service 层", ["封装核心业务规则", "事务控制", "状态流转与权限判断"], GREEN)
package(1780, 1040, 470, 220, "Mapper/Entity 层", ["MyBatis-Plus Mapper", "实体对象", "数据库读写"], GREEN)
package(2330, 1040, 470, 220, "扩展能力", ["推荐服务", "敏感词过滤", "举报处理"], GREEN)

# Data resources
package(190, 1600, 430, 140, "Redis", ["验证码、登录限流", "JWT 黑名单、商品热度"], YELLOW)
package(800, 1600, 430, 140, "uploads", ["商品图片", "用户头像"], RED)
package(1410, 1600, 520, 140, "MySQL", ["users / products / orders / messages", "favorites / reports / audit_logs"], YELLOW)
package(2110, 1600, 430, 140, "Knife4j/OpenAPI", ["接口文档", "接口调试"], RED)

# Clean main dependency arrows
arrow(1985, 350, 1985, 520, "REST API")
arrow(1120, 1150, 1200, 1150, "调用")
arrow(1700, 1150, 1780, 1150, "读写")
arrow(2015, 1260, 1670, 1600, "Mapper/JDBC")
arrow(365, 1420, 405, 1600, "缓存/限流")
arrow(1415, 1420, 1015, 1600, "文件上传")
arrow(2530, 1420, 2325, 1600, "接口文档")

img.save(OUT, dpi=(600, 600))
print(OUT.resolve())
