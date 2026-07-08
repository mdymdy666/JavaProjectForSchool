#!/bin/bash
# 演示数据种子脚本 — 通过 API 插入演示商品、订单、消息、公告
API="http://localhost:8080/api"

echo "=== 1. 注册演示卖家 ==="
curl -s -X POST $API/auth/register -H "Content-Type: application/json" \
  -d '{"username":"xiaoli","password":"Pass1234","nickname":"计科小李"}' | grep -o '"id":[0-9]*' | head -1
curl -s -X POST $API/auth/register -H "Content-Type: application/json" \
  -d '{"username":"xiaozhou","password":"Pass1234","nickname":"经管小周"}' | grep -o '"id":[0-9]*' | head -1
curl -s -X POST $API/auth/register -H "Content-Type: application/json" \
  -d '{"username":"xiaochen","password":"Pass1234","nickname":"软件小陈"}' | grep -o '"id":[0-9]*' | head -1

echo ""
echo "=== 2. 登录获取 tokens ==="
TOKEN_LI=$(curl -s -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"account":"xiaoli","password":"Pass1234"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
TOKEN_ZHOU=$(curl -s -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"account":"xiaozhou","password":"Pass1234"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
TOKEN_CHEN=$(curl -s -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"account":"xiaochen","password":"Pass1234"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
TOKEN_ADMIN=$(curl -s -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"admin123"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
echo "Got tokens: Li=${#TOKEN_LI} Zhou=${#TOKEN_ZHOU} Chen=${#TOKEN_CHEN} Admin=${#TOKEN_ADMIN}"

echo ""
echo "=== 3. 发布商品 ==="
# 小李 — 数码配件
P1=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_LI" -H "Content-Type: application/json" \
  -d '{"title":"Cherry 机械键盘 MX3.0S","categoryId":1,"price":129,"itemCondition":"九成新","description":"Cherry 原厂轴体，送拔键器，适合编程和游戏。用了半年，包装盒还在。","imageUrls":["https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P1(键盘)=$P1"

P2=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_LI" -H "Content-Type: application/json" \
  -d '{"title":"罗技 G502 游戏鼠标","categoryId":1,"price":89,"itemCondition":"八成新","description":"Hero 传感器，可调配重，适合 FPS 和 MOBA 游戏。侧裙轻微磨损。","imageUrls":["https://images.unsplash.com/photo-1527814050087-3793815479db?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P2(鼠标)=$P2"

# 小周 — 图书教材
P3=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_ZHOU" -H "Content-Type: application/json" \
  -d '{"title":"2026 考研数学复习全书","categoryId":2,"price":35,"itemCondition":"轻微笔记","description":"2026 最新版，重点章节用荧光笔做了标注，不影响阅读。附赠自己整理的错题本。","imageUrls":["https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P3(考研数学)=$P3"

P4=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_ZHOU" -H "Content-Type: application/json" \
  -d '{"title":"数据结构（C语言版）严蔚敏","categoryId":2,"price":22,"itemCondition":"七成新","description":"经典教材，封面有折痕，内页干净无笔记。计算机专业必备用书。","imageUrls":["https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P4(数据结构)=$P4"

# 小陈 — 生活用品
P5=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_CHEN" -H "Content-Type: application/json" \
  -d '{"title":"宿舍折叠桌 床上书桌","categoryId":3,"price":48,"itemCondition":"八成新","description":"可放笔记本电脑，带杯架和 iPad 支架。宿舍自提，不包邮。","imageUrls":["https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P5(折叠桌)=$P5"

P6=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_CHEN" -H "Content-Type: application/json" \
  -d '{"title":"小米台灯 1S 护眼版","categoryId":3,"price":65,"itemCondition":"九成新","description":"LED 护眼台灯，支持色温调节和手机控制。使用不到一年。","imageUrls":["https://images.unsplash.com/photo-1507473885765-e6ed057ab6fe?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P6(台灯)=$P6"

# 运动户外 + 服饰鞋包
P7=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_LI" -H "Content-Type: application/json" \
  -d '{"title":"YONEX 羽毛球拍 NR-700","categoryId":4,"price":150,"itemCondition":"八成新","description":"入门级羽毛球拍，送原装拍套和 3 个羽毛球。拍线刚换过。","imageUrls":["https://images.unsplash.com/photo-1613918431703-aa50889e3be9?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P7(羽毛球拍)=$P7"

P8=$(curl -s -X POST $API/products -H "Authorization: Bearer $TOKEN_ZHOU" -H "Content-Type: application/json" \
  -d '{"title":"CHAMPION 连帽卫衣 L码","categoryId":5,"price":79,"itemCondition":"九成新","description":"正品 CHAMPION 连帽卫衣，灰色 L 码。只穿了两三次，洗过一次。","imageUrls":["https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600"]}' | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "P8(卫衣)=$P8"

echo ""
echo "=== 4. 管理员审核商品 ==="
for pid in $P1 $P2 $P3 $P4 $P5 $P6 $P7 $P8; do
  curl -s -X POST "$API/admin/products/$pid/audit" -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
    -d '{"approved":true,"reason":"信息完整，图片清晰"}' > /dev/null
  echo "Approved product $pid"
done

echo ""
echo "=== 5. 创建订单并推进状态 ==="
# 订单1: 小陈买小李的键盘 PENDING_PAYMENT -> PAID -> SHIPPED
O1=$(curl -s -X POST $API/orders -H "Authorization: Bearer $TOKEN_CHEN" -H "Content-Type: application/json" \
  -d "{\"productId\":$P1}" | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "O1(键盘订单)=$O1"
curl -s -X POST "$API/orders/$O1/pay" -H "Authorization: Bearer $TOKEN_CHEN" > /dev/null && echo "O1 paid"
curl -s -X POST "$API/orders/$O1/ship" -H "Authorization: Bearer $TOKEN_LI" -H "Content-Type: application/json" \
  -d '{"logisticsInfo":"申通 7730123456789"}' > /dev/null && echo "O1 shipped"

# 订单2: 小李买小周的考研书 PAID
O2=$(curl -s -X POST $API/orders -H "Authorization: Bearer $TOKEN_LI" -H "Content-Type: application/json" \
  -d "{\"productId\":$P3}" | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "O2(考研书订单)=$O2"
curl -s -X POST "$API/orders/$O2/pay" -H "Authorization: Bearer $TOKEN_LI" > /dev/null && echo "O2 paid"

# 订单3: 小周买小陈的折叠桌 COMPLETED
O3=$(curl -s -X POST $API/orders -H "Authorization: Bearer $TOKEN_ZHOU" -H "Content-Type: application/json" \
  -d "{\"productId\":$P5}" | sed 's/.*"id":\([0-9]*\).*/\1/')
echo "O3(折叠桌订单)=$O3"
curl -s -X POST "$API/orders/$O3/pay" -H "Authorization: Bearer $TOKEN_ZHOU" > /dev/null && echo "O3 paid"
curl -s -X POST "$API/orders/$O3/ship" -H "Authorization: Bearer $TOKEN_CHEN" -H "Content-Type: application/json" \
  -d '{"logisticsInfo":"图书馆门口自提"}' > /dev/null && echo "O3 shipped"
curl -s -X POST "$API/orders/$O3/confirm" -H "Authorization: Bearer $TOKEN_ZHOU" > /dev/null && echo "O3 completed"

echo ""
echo "=== 6. 添加留言 ==="
curl -s -X POST $API/messages -H "Authorization: Bearer $TOKEN_CHEN" -H "Content-Type: application/json" \
  -d "{\"receiverId\":$(curl -s $API/products/$P1 -H "Authorization: Bearer $TOKEN_CHEN" | sed 's/.*"sellerId":\([0-9]*\).*/\1/'),\"productId\":$P1,\"content\":\"键盘今晚 7 点在图书馆门口交易可以吗？\"}" > /dev/null && echo "Message 1 sent"
curl -s -X POST $API/messages -H "Authorization: Bearer $TOKEN_LI" -H "Content-Type: application/json" \
  -d "{\"receiverId\":$(curl -s $API/products/$P1 -H "Authorization: Bearer $TOKEN_LI" | sed 's/.*"sellerId":\([0-9]*\).*/\1/'),\"productId\":$P1,\"content\":\"好的，没问题。包装盒和拔键器我都带着。\"}" > /dev/null && echo "Message 2 sent"

echo ""
echo "=== 7. 发布公告 ==="
curl -s -X POST $API/admin/announcements -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"title":"欢迎使用校园二手交易平台","content":"本平台仅供校内师生使用。交易请选择校内公共场所，注意人身和财产安全。如遇纠纷请联系管理员处理。","published":true}' > /dev/null && echo "Announcement 1 created"
curl -s -X POST $API/admin/announcements -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"title":"毕业季专场活动","content":"6月-7月毕业季期间，发布商品免审核快速上架。欢迎毕业生踊跃发布闲置物品！","published":true}' > /dev/null && echo "Announcement 2 created"

echo ""
echo "=== 演示数据创建完成 ==="
echo "账号: admin/admin123, xiaoli/Pass1234, xiaozhou/Pass1234, xiaochen/Pass1234"
echo "商品: 8个 (已审核) | 订单: 3个 | 公告: 2条 | 留言: 2条"
