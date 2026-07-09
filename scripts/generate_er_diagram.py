from PIL import Image, ImageDraw, ImageFont
from pathlib import Path


OUT = Path("campus_trade_er_diagram_plain.png")
SCALE = 4
W, H = 2400 * SCALE, 1600 * SCALE


def font(size, bold=False):
    size = int(size * SCALE)
    candidates = [
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\calibrib.ttf" if bold else r"C:\Windows\Fonts\calibri.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
    ]
    for item in candidates:
        if Path(item).exists():
            return ImageFont.truetype(item, size)
    return ImageFont.load_default()


TITLE = font(44, True)
SUBTITLE = font(22)
TABLE = font(24, True)
FIELD = font(18)
SMALL = font(16)
LABEL = font(16, True)


tables = {
    "users": {
        "xy": (80, 200),
        "fields": [
            "PK id",
            "username UNIQUE",
            "password_hash",
            "nickname",
            "phone, email",
            "avatar_url",
            "role, status",
            "created_at, updated_at",
        ],
    },
    "user_addresses": {
        "xy": (80, 1160),
        "fields": [
            "PK id",
            "FK user_id -> users.id",
            "receiver_name",
            "receiver_phone",
            "detail_address",
            "is_default",
            "created_at",
        ],
    },
    "categories": {
        "xy": (680, 180),
        "fields": [
            "PK id",
            "name UNIQUE",
            "sort_order",
            "enabled",
        ],
    },
    "products": {
        "xy": (680, 430),
        "fields": [
            "PK id",
            "FK seller_id -> users.id",
            "FK category_id -> categories.id",
            "title",
            "description",
            "price",
            "item_condition",
            "status",
            "view_count",
            "version",
            "deleted",
            "created_at, updated_at",
        ],
    },
    "product_images": {
        "xy": (1280, 180),
        "fields": [
            "PK id",
            "FK product_id -> products.id",
            "image_url",
            "sort_order",
        ],
    },
    "favorites": {
        "xy": (1280, 420),
        "fields": [
            "PK id",
            "FK user_id -> users.id",
            "FK product_id -> products.id",
            "UNIQUE(user_id, product_id)",
            "created_at",
        ],
    },
    "user_behaviors": {
        "xy": (1280, 700),
        "fields": [
            "PK id",
            "FK user_id -> users.id",
            "FK product_id -> products.id",
            "behavior_type",
            "keyword",
            "FK category_id -> categories.id",
            "weight",
            "created_at",
        ],
    },
    "orders": {
        "xy": (680, 880),
        "fields": [
            "PK id",
            "order_no UNIQUE",
            "FK buyer_id -> users.id",
            "FK seller_id -> users.id",
            "FK product_id -> products.id",
            "total_amount",
            "status",
            "logistics_info",
            "version",
            "deleted",
            "created_at, updated_at",
        ],
    },
    "messages": {
        "xy": (80, 540),
        "fields": [
            "PK id",
            "FK sender_id -> users.id",
            "FK receiver_id -> users.id",
            "FK product_id -> products.id",
            "content",
            "status",
            "created_at",
        ],
    },
    "order_logs": {
        "xy": (1280, 1040),
        "fields": [
            "PK id",
            "FK order_id -> orders.id",
            "from_status",
            "to_status",
            "FK operator_id -> users.id",
            "remark",
            "created_at",
        ],
    },
    "notifications": {
        "xy": (80, 850),
        "fields": [
            "PK id",
            "FK user_id -> users.id",
            "type",
            "title",
            "content",
            "read_status",
            "created_at",
        ],
    },
    "announcements": {
        "xy": (1880, 420),
        "fields": [
            "PK id",
            "title",
            "content",
            "published",
            "created_at",
        ],
    },
    "audit_logs": {
        "xy": (1280, 1350),
        "fields": [
            "PK id",
            "FK admin_id -> users.id",
            "target_type",
            "target_id",
            "action",
            "reason",
            "created_at",
        ],
    },
    "reports": {
        "xy": (680, 1350),
        "fields": [
            "PK id",
            "FK reporter_id -> users.id",
            "FK product_id -> products.id",
            "reason",
            "report_status",
            "FK admin_id -> users.id",
            "handling_result",
            "processed_at",
            "created_at",
        ],
    },
    "sensitive_words": {
        "xy": (1880, 700),
        "fields": [
            "PK id",
            "word UNIQUE",
            "enabled",
        ],
    },
}

for data in tables.values():
    x, y = data["xy"]
    data["xy"] = (x * SCALE, (y - 120) * SCALE)

BOX_W = 430 * SCALE
HEADER_H = 42 * SCALE
LINE_H = 25 * SCALE
PAD = 16 * SCALE


def box_rect(name):
    x, y = tables[name]["xy"]
    h = HEADER_H + PAD + len(tables[name]["fields"]) * LINE_H + 14
    return (x, y, x + BOX_W, y + h)


def anchor(name, side):
    x1, y1, x2, y2 = box_rect(name)
    if side == "right":
        return (x2, (y1 + y2) // 2)
    if side == "left":
        return (x1, (y1 + y2) // 2)
    if side == "top":
        return ((x1 + x2) // 2, y1)
    return ((x1 + x2) // 2, y2)


img = Image.new("RGB", (W, H), "#f7f9fb")
draw = ImageDraw.Draw(img)

def draw_card(name, data):
    x, y = data["xy"]
    x2, y2 = box_rect(name)[2:]
    draw.rounded_rectangle((x, y, x2, y2), radius=18 * SCALE, fill="white", outline="#9fb5c8", width=2 * SCALE)
    draw.rounded_rectangle((x, y, x2, y + HEADER_H), radius=18 * SCALE, fill="#2f6f9f", outline="#2f6f9f")
    draw.rectangle((x, y + HEADER_H - 16 * SCALE, x2, y + HEADER_H), fill="#2f6f9f")
    draw.text((x + PAD, y + 9 * SCALE), name, font=TABLE, fill="white")
    current_y = y + HEADER_H + 12 * SCALE
    for field in data["fields"]:
        color = "#17324d" if field.startswith("PK") else "#25445f"
        if field.startswith("FK"):
            color = "#8a4b00"
        draw.text((x + PAD, current_y), field, font=FIELD, fill=color)
        current_y += LINE_H


def polyline(points, fill="#6f8191", width=3):
    draw.line(points, fill=fill, width=width, joint="curve")


def relation(src, dst, label, src_side="right", dst_side="left", color="#6f8191"):
    sx, sy = anchor(src, src_side)
    dx, dy = anchor(dst, dst_side)
    mid_x = (sx + dx) // 2
    if src_side in ("top", "bottom") or dst_side in ("top", "bottom"):
        mid_y = (sy + dy) // 2
        points = [(sx, sy), (sx, mid_y), (dx, mid_y), (dx, dy)]
    else:
        points = [(sx, sy), (mid_x, sy), (mid_x, dy), (dx, dy)]
    polyline(points, color, 3 * SCALE)
    lx = sum(p[0] for p in points) // len(points)
    ly = sum(p[1] for p in points) // len(points)
    tw = draw.textlength(label, font=LABEL)
    draw.rounded_rectangle(
        (lx - tw / 2 - 7 * SCALE, ly - 12 * SCALE, lx + tw / 2 + 7 * SCALE, ly + 13 * SCALE),
        radius=7 * SCALE,
        fill="#f7f9fb",
    )
    draw.text((lx - tw / 2, ly - 10 * SCALE), label, font=LABEL, fill=color)


# Draw relations behind cards.
relations = [
    ("categories", "products", "1 : N", "right", "left"),
    ("users", "products", "seller 1 : N", "right", "left"),
    ("products", "product_images", "1 : N", "right", "left"),
    ("users", "favorites", "1 : N", "right", "left"),
    ("products", "favorites", "1 : N", "right", "left"),
    ("users", "user_behaviors", "1 : N", "right", "left"),
    ("products", "user_behaviors", "1 : N", "right", "left"),
    ("categories", "user_behaviors", "1 : N", "right", "left"),
    ("users", "orders", "buyer/seller 1 : N", "right", "left"),
    ("products", "orders", "1 : N", "bottom", "top"),
    ("orders", "order_logs", "1 : N", "right", "left"),
    ("users", "order_logs", "operator 1 : N", "right", "left"),
    ("users", "messages", "sender/receiver 1 : N", "bottom", "top"),
    ("products", "messages", "1 : N", "left", "right"),
    ("users", "notifications", "1 : N", "bottom", "top"),
    ("users", "user_addresses", "1 : N", "bottom", "top"),
    ("users", "audit_logs", "admin 1 : N", "right", "left"),
    ("users", "reports", "reporter/admin 1 : N", "right", "left"),
    ("products", "reports", "1 : N", "bottom", "top"),
]
for item in relations:
    relation(*item)

for name, data in tables.items():
    draw_card(name, data)

img.save(OUT, dpi=(600, 600))
print(OUT.resolve())
