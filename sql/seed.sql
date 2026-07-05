USE campus_trade;

INSERT INTO categories (name, sort_order, enabled) VALUES
  ('数码配件', 10, 1),
  ('图书教材', 20, 1),
  ('生活用品', 30, 1),
  ('运动户外', 40, 1),
  ('服饰鞋包', 50, 1)
ON DUPLICATE KEY UPDATE
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);

INSERT INTO users (username, password_hash, nickname, role, status)
VALUES ('admin', '$2a$10$replace_with_real_bcrypt_hash', '系统管理员', 'ADMIN', 'NORMAL')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  role = VALUES(role),
  status = VALUES(status);
