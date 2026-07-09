from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path("campus_trade_er_diagram_neat.png")
SCALE = 3
W, H = 3600 * SCALE, 2300 * SCALE


def load_font(size, bold=False):
    size = int(size * SCALE)
    fonts = [
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\calibrib.ttf" if bold else r"C:\Windows\Fonts\calibri.ttf",
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
    ]
    for item in fonts:
        if Path(item).exists():
            return ImageFont.truetype(item, size)
    return ImageFont.load_default()


FONT_TITLE = load_font(20, True)
FONT_FIELD = load_font(15)
FONT_KEY = load_font(15, True)
FONT_REL = load_font(13, True)

BG = "#ffffff"
BOX = "#ffffff"
HEADER = "#dbeafe"
BORDER = "#7c8fb3"
TEXT = "#203044"
FK = "#8a4b00"
LINE = "#9aa8bd"

BOX_W = 430 * SCALE
HEADER_H = 34 * SCALE
ROW_H = 23 * SCALE
PAD_X = 14 * SCALE
PAD_Y = 9 * SCALE


tables = {
    "users": {
        "xy": (90, 120),
        "fields": [
            ("PK", "id"),
            ("UQ", "username"),
            ("", "password_hash"),
            ("", "nickname"),
            ("", "phone, email"),
            ("", "avatar_url"),
            ("", "role, status"),
            ("", "created_at, updated_at"),
        ],
    },
    "messages": {
        "xy": (90, 560),
        "fields": [
            ("PK", "id"),
            ("FK", "sender_id -> users.id"),
            ("FK", "receiver_id -> users.id"),
            ("FK", "product_id -> products.id"),
            ("", "content"),
            ("", "status"),
            ("", "created_at"),
        ],
    },
    "notifications": {
        "xy": (90, 920),
        "fields": [
            ("PK", "id"),
            ("FK", "user_id -> users.id"),
            ("", "type"),
            ("", "title"),
            ("", "content"),
            ("", "read_status"),
            ("", "created_at"),
        ],
    },
    "user_addresses": {
        "xy": (90, 1280),
        "fields": [
            ("PK", "id"),
            ("FK", "user_id -> users.id"),
            ("", "receiver_name"),
            ("", "receiver_phone"),
            ("", "detail_address"),
            ("", "is_default"),
            ("", "created_at"),
        ],
    },
    "categories": {
        "xy": (670, 120),
        "fields": [
            ("PK", "id"),
            ("UQ", "name"),
            ("", "sort_order"),
            ("", "enabled"),
        ],
    },
    "products": {
        "xy": (670, 410),
        "fields": [
            ("PK", "id"),
            ("FK", "seller_id -> users.id"),
            ("FK", "category_id -> categories.id"),
            ("", "title"),
            ("", "description"),
            ("", "price"),
            ("", "item_condition"),
            ("", "status"),
            ("", "view_count"),
            ("", "version"),
            ("", "deleted"),
            ("", "created_at, updated_at"),
        ],
    },
    "orders": {
        "xy": (670, 910),
        "fields": [
            ("PK", "id"),
            ("UQ", "order_no"),
            ("FK", "buyer_id -> users.id"),
            ("FK", "seller_id -> users.id"),
            ("FK", "product_id -> products.id"),
            ("", "total_amount"),
            ("", "status"),
            ("", "logistics_info"),
            ("", "version"),
            ("", "deleted"),
            ("", "created_at, updated_at"),
        ],
    },
    "reports": {
        "xy": (670, 1390),
        "fields": [
            ("PK", "id"),
            ("FK", "reporter_id -> users.id"),
            ("FK", "product_id -> products.id"),
            ("", "reason"),
            ("", "report_status"),
            ("FK", "admin_id -> users.id"),
            ("", "handling_result"),
            ("", "processed_at"),
            ("", "created_at"),
        ],
    },
    "product_images": {
        "xy": (1250, 120),
        "fields": [
            ("PK", "id"),
            ("FK", "product_id -> products.id"),
            ("", "image_url"),
            ("", "sort_order"),
        ],
    },
    "favorites": {
        "xy": (1250, 410),
        "fields": [
            ("PK", "id"),
            ("FK", "user_id -> users.id"),
            ("FK", "product_id -> products.id"),
            ("UQ", "user_id, product_id"),
            ("", "created_at"),
        ],
    },
    "user_behaviors": {
        "xy": (1250, 720),
        "fields": [
            ("PK", "id"),
            ("FK", "user_id -> users.id"),
            ("FK", "product_id -> products.id"),
            ("", "behavior_type"),
            ("", "keyword"),
            ("FK", "category_id -> categories.id"),
            ("", "weight"),
            ("", "created_at"),
        ],
    },
    "order_logs": {
        "xy": (1250, 1090),
        "fields": [
            ("PK", "id"),
            ("FK", "order_id -> orders.id"),
            ("", "from_status"),
            ("", "to_status"),
            ("FK", "operator_id -> users.id"),
            ("", "remark"),
            ("", "created_at"),
        ],
    },
    "audit_logs": {
        "xy": (1250, 1450),
        "fields": [
            ("PK", "id"),
            ("FK", "admin_id -> users.id"),
            ("", "target_type"),
            ("", "target_id"),
            ("", "action"),
            ("", "reason"),
            ("", "created_at"),
        ],
    },
    "announcements": {
        "xy": (1830, 410),
        "fields": [
            ("PK", "id"),
            ("", "title"),
            ("", "content"),
            ("", "published"),
            ("", "created_at"),
        ],
    },
    "sensitive_words": {
        "xy": (1830, 720),
        "fields": [
            ("PK", "id"),
            ("UQ", "word"),
            ("", "enabled"),
        ],
    },
}

for table in tables.values():
    x, y = table["xy"]
    table["xy"] = (x * SCALE, y * SCALE)


def rect(name):
    x, y = tables[name]["xy"]
    height = HEADER_H + PAD_Y + len(tables[name]["fields"]) * ROW_H + PAD_Y
    return x, y, x + BOX_W, y + height


def anchor(name, side, offset=0):
    x1, y1, x2, y2 = rect(name)
    if side == "left":
        return x1, (y1 + y2) // 2 + offset * SCALE
    if side == "right":
        return x2, (y1 + y2) // 2 + offset * SCALE
    if side == "top":
        return (x1 + x2) // 2 + offset * SCALE, y1
    return (x1 + x2) // 2 + offset * SCALE, y2


def draw_table(draw, name):
    x1, y1, x2, y2 = rect(name)
    draw.rounded_rectangle((x1, y1, x2, y2), radius=10 * SCALE, fill=BOX, outline=BORDER, width=2 * SCALE)
    draw.rounded_rectangle((x1, y1, x2, y1 + HEADER_H), radius=10 * SCALE, fill=HEADER, outline=BORDER, width=2 * SCALE)
    draw.rectangle((x1, y1 + HEADER_H - 10 * SCALE, x2, y1 + HEADER_H), fill=HEADER)
    draw.text((x1 + PAD_X, y1 + 5 * SCALE), name, font=FONT_TITLE, fill=TEXT)
    y = y1 + HEADER_H + PAD_Y
    for key, field in tables[name]["fields"]:
        key_color = FK if key == "FK" else TEXT
        field_color = FK if key == "FK" else TEXT
        if key:
            draw.text((x1 + PAD_X, y), key, font=FONT_KEY, fill=key_color)
            draw.text((x1 + PAD_X + 48 * SCALE, y), field, font=FONT_FIELD, fill=field_color)
        else:
            draw.text((x1 + PAD_X + 48 * SCALE, y), field, font=FONT_FIELD, fill=field_color)
        y += ROW_H


def draw_label(draw, x, y, text):
    text_w = draw.textlength(text, font=FONT_REL)
    draw.rounded_rectangle(
        (x - text_w / 2 - 5 * SCALE, y - 10 * SCALE, x + text_w / 2 + 5 * SCALE, y + 10 * SCALE),
        radius=4 * SCALE,
        fill=BG,
    )
    draw.text((x - text_w / 2, y - 9 * SCALE), text, font=FONT_REL, fill=LINE)


def route(draw, points, label=None, label_at=0.5):
    draw.line(points, fill=LINE, width=1 * SCALE, joint="curve")
    if label:
        total = len(points) - 1
        idx = min(total - 1, max(0, int(total * label_at)))
        x = (points[idx][0] + points[idx + 1][0]) // 2
        y = (points[idx][1] + points[idx + 1][1]) // 2
        draw_label(draw, x, y, label)


def connect(draw, src, dst, label="", src_side="right", dst_side="left", corridor=None, src_offset=0, dst_offset=0):
    sx, sy = anchor(src, src_side, src_offset)
    dx, dy = anchor(dst, dst_side, dst_offset)
    if corridor is None:
        if src_side in ("left", "right") and dst_side in ("left", "right"):
            corridor = (sx + dx) // 2
            points = [(sx, sy), (corridor, sy), (corridor, dy), (dx, dy)]
        else:
            corridor = (sy + dy) // 2
            points = [(sx, sy), (sx, corridor), (dx, corridor), (dx, dy)]
    elif src_side in ("left", "right") and dst_side in ("left", "right"):
        c = corridor * SCALE
        points = [(sx, sy), (c, sy), (c, dy), (dx, dy)]
    else:
        c = corridor * SCALE
        points = [(sx, sy), (sx, c), (dx, c), (dx, dy)]
    route(draw, points, label)


img = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img)

# Relationships first, so entity boxes stay visually on top.
connect(draw, "users", "products", src_side="right", dst_side="left", corridor=560, src_offset=-30, dst_offset=-40)
connect(draw, "categories", "products", src_side="bottom", dst_side="top", corridor=345)
connect(draw, "products", "product_images", src_side="right", dst_side="left", corridor=1160, src_offset=-120)
connect(draw, "users", "messages", src_side="bottom", dst_side="top", corridor=490)
connect(draw, "users", "notifications", src_side="bottom", dst_side="top", corridor=490)
connect(draw, "users", "user_addresses", src_side="bottom", dst_side="top", corridor=490)
connect(draw, "users", "orders", src_side="right", dst_side="left", corridor=560, src_offset=30, dst_offset=-70)
connect(draw, "products", "orders", src_side="bottom", dst_side="top", corridor=760)
connect(draw, "products", "favorites", src_side="right", dst_side="left", corridor=1135, src_offset=-35)
connect(draw, "users", "favorites", src_side="right", dst_side="left", corridor=1120, dst_offset=-45)
connect(draw, "products", "user_behaviors", src_side="right", dst_side="left", corridor=1135, src_offset=60)
connect(draw, "users", "user_behaviors", src_side="right", dst_side="left", corridor=1100, src_offset=70, dst_offset=-45)
connect(draw, "categories", "user_behaviors", src_side="right", dst_side="left", corridor=1165, dst_offset=40)
connect(draw, "orders", "order_logs", src_side="right", dst_side="left", corridor=1160, src_offset=-15)
connect(draw, "users", "order_logs", src_side="right", dst_side="left", corridor=1090, src_offset=120, dst_offset=-35)
connect(draw, "products", "messages", src_side="left", dst_side="right", corridor=545, src_offset=-20, dst_offset=25)
connect(draw, "products", "reports", src_side="bottom", dst_side="top", corridor=1320)
connect(draw, "users", "reports", src_side="right", dst_side="left", corridor=560, src_offset=125)
connect(draw, "users", "audit_logs", src_side="right", dst_side="left", corridor=1080, src_offset=150, dst_offset=-30)

for table_name in tables:
    draw_table(draw, table_name)

# Crop to content bounding box with consistent margins.
margin = 70 * SCALE
x_min = min(rect(name)[0] for name in tables) - margin
y_min = min(rect(name)[1] for name in tables) - margin
x_max = max(rect(name)[2] for name in tables) + margin
y_max = max(rect(name)[3] for name in tables) + margin
img = img.crop((max(0, x_min), max(0, y_min), min(W, x_max), min(H, y_max)))
img.save(OUT, dpi=(600, 600))
print(OUT.resolve())
