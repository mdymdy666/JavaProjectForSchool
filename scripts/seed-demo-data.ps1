$api = "http://localhost:8080/api"
$ct = "application/json"

function Post($uri, $body) { Invoke-RestMethod -Uri "$api$uri" -Method Post -ContentType $ct -Body $body }
function PostAuth($token, $uri, $body) {
    Invoke-RestMethod -Uri "$api$uri" -Method Post -ContentType $ct -Body $body -Headers @{Authorization="Bearer $token"}
}

Write-Output "=== 1. Register sellers ==="
$r1 = Post "/auth/register" '{"username":"xiaoli","password":"Pass1234","nickname":"计科小李"}'
$r2 = Post "/auth/register" '{"username":"xiaozhou","password":"Pass1234","nickname":"经管小周"}'
$r3 = Post "/auth/register" '{"username":"xiaochen","password":"Pass1234","nickname":"软件小陈"}'
Write-Output "Users: $($r1.data.id), $($r2.data.id), $($r3.data.id)"

Write-Output "`n=== 2. Login ==="
$t1 = (Post "/auth/login" '{"account":"xiaoli","password":"Pass1234"}').data.accessToken
$t2 = (Post "/auth/login" '{"account":"xiaozhou","password":"Pass1234"}').data.accessToken
$t3 = (Post "/auth/login" '{"account":"xiaochen","password":"Pass1234"}').data.accessToken
$ta = (Post "/auth/login" '{"account":"admin","password":"admin123"}').data.accessToken

Write-Output "`n=== 3. Publish products ==="
$p1 = (PostAuth $t1 "/products" '{"title":"Cherry 机械键盘 MX3.0S","categoryId":1,"price":129,"itemCondition":"九成新","description":"Cherry 原厂轴体，送拔键器，适合编程和游戏。用了半年，包装盒还在。","imageUrls":["https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600"]}').data.id
Write-Output "P1(键盘)=$p1"
$p2 = (PostAuth $t1 "/products" '{"title":"罗技 G502 游戏鼠标","categoryId":1,"price":89,"itemCondition":"八成新","description":"Hero 传感器，可调配重，适合 FPS 和 MOBA。侧裙轻微磨损。","imageUrls":["https://images.unsplash.com/photo-1527814050087-3793815479db?w=600"]}').data.id
Write-Output "P2(鼠标)=$p2"
$p3 = (PostAuth $t2 "/products" '{"title":"2026 考研数学复习全书","categoryId":2,"price":35,"itemCondition":"轻微笔记","description":"2026 最新版，重点章节荧光笔标注。附赠错题本。","imageUrls":["https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600"]}').data.id
Write-Output "P3(考研)=$p3"
$p4 = (PostAuth $t2 "/products" '{"title":"数据结构 C语言版 严蔚敏","categoryId":2,"price":22,"itemCondition":"七成新","description":"经典教材，封面有折痕，内页干净。计算机专业必备用书。","imageUrls":["https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600"]}').data.id
Write-Output "P4(数据结构)=$p4"
$p5 = (PostAuth $t3 "/products" '{"title":"宿舍折叠桌 床上书桌","categoryId":3,"price":48,"itemCondition":"八成新","description":"可放笔记本，带杯架和iPad支架。宿舍自提。","imageUrls":["https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=600"]}').data.id
Write-Output "P5(折叠桌)=$p5"
$p6 = (PostAuth $t3 "/products" '{"title":"小米台灯 1S 护眼版","categoryId":3,"price":65,"itemCondition":"九成新","description":"LED护眼台灯，支持色温调节和手机控制。使用不到一年。","imageUrls":["https://images.unsplash.com/photo-1507473885765-e6ed057ab6fe?w=600"]}').data.id
Write-Output "P6(台灯)=$p6"
$p7 = (PostAuth $t1 "/products" '{"title":"YONEX 羽毛球拍 NR-700","categoryId":4,"price":150,"itemCondition":"八成新","description":"入门级羽毛球拍，送原装拍套和3个羽毛球。拍线刚换过。","imageUrls":["https://images.unsplash.com/photo-1613918431703-aa50889e3be9?w=600"]}').data.id
Write-Output "P7(羽毛球拍)=$p7"
$p8 = (PostAuth $t2 "/products" '{"title":"CHAMPION 连帽卫衣 L码","categoryId":5,"price":79,"itemCondition":"九成新","description":"正品CHAMPION连帽卫衣，灰色L码。穿过两三次。","imageUrls":["https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600"]}').data.id
Write-Output "P8(卫衣)=$p8"

Write-Output "`n=== 4. Admin audit ==="
foreach ($pid in @($p1,$p2,$p3,$p4,$p5,$p6,$p7,$p8)) {
    PostAuth $ta "/admin/products/$pid/audit" '{"approved":true,"reason":"信息完整"}'
    Write-Output "Approved $pid"
}

Write-Output "`n=== 5. Orders ==="
$o1 = (PostAuth $t3 "/orders" "{`"productId`":$p1}").data.id
PostAuth $t3 "/orders/$o1/pay" '{}'; PostAuth $t1 "/orders/$o1/ship" '{"logisticsInfo":"申通 7730123456789"}'
Write-Output "O1(键盘)=$o1: PAID->SHIPPED"

$o2 = (PostAuth $t1 "/orders" "{`"productId`":$p3}").data.id
PostAuth $t1 "/orders/$o2/pay" '{}'
Write-Output "O2(考研书)=$o2: PAID"

$o3 = (PostAuth $t2 "/orders" "{`"productId`":$p5}").data.id
PostAuth $t2 "/orders/$o3/pay" '{}'; PostAuth $t3 "/orders/$o3/ship" '{"logisticsInfo":"图书馆门口自提"}'; PostAuth $t2 "/orders/$o3/confirm" '{}'
Write-Output "O3(折叠桌)=$o3: COMPLETED"

Write-Output "`n=== 6. Messages ==="
$sid = (Invoke-RestMethod -Uri "$api/products/$p1").data.sellerId
PostAuth $t3 "/messages" "{`"receiverId`":$sid,`"productId`":$p1,`"content`":`"键盘今晚7点在图书馆门口交易可以吗？`"}"
PostAuth $t1 "/messages" "{`"receiverId`":$($r3.data.id),`"productId`":$p1,`"content`":`"没问题，包装盒和拔键器我都带着。`"}"
Write-Output "2 messages sent"

Write-Output "`n=== 7. Announcements ==="
PostAuth $ta "/admin/announcements" '{"title":"欢迎使用校园二手交易平台","content":"本平台仅供校内师生使用。交易请选择校内公共场所，注意人身和财产安全。","published":true}'
PostAuth $ta "/admin/announcements" '{"title":"毕业季专场活动","content":"6-7月毕业季期间，欢迎毕业生踊跃发布闲置物品！","published":true}'
Write-Output "2 announcements created"

Write-Output "`n=== DONE ==="
Write-Output "Users: admin/admin123 | xiaoli/Pass1234 | xiaozhou/Pass1234 | xiaochen/Pass1234"
Write-Output "Products: 8 | Orders: 3 | Announcements: 2 | Messages: 2"
