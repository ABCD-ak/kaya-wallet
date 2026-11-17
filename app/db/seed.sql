-- =============================================================
-- 基础分类种子数据（仅首次初始化执行）
-- 执行：mysql -u root -p kaya < db/seed.sql
-- =============================================================

SET NAMES utf8mb4;

INSERT INTO categories (code, type, name, icon, color, sort_order) VALUES
  ('food_drink',    'expense', '餐饮',     'ic-food-drink',      '#f97316', 10),
  ('fashion',       'expense', '服饰',     'ic-fashion',         '#ec4899', 20),
  ('travelling',    'expense', '旅行',     'ic-travelling',      '#8b5cf6', 30),
  ('house',         'expense', '住房',     'ic-house',           '#3b82f6', 40),
  ('shopping',      'expense', '购物',     'ic-see-more',        '#22c55e', 50),
  ('entertainment', 'expense', '娱乐',     'ic-invoice-payment', '#eab308', 60),
  ('transport',     'expense', '交通',     'ic-address',         '#06b6d4', 70),
  ('other_expense', 'expense', '其他',     'ic-see-more',        '#9ca3af', 999),
  ('salary',        'income',  '工资',     'ic-income',          '#17b7bd', 10),
  ('bonus',         'income',  '奖金',     'ic-add-income',      '#9deb93', 20),
  ('refund',        'income',  '退款',     'ic-correct',         '#67b3f3', 30),
  ('other_income',  'income',  '其他收入', 'ic-see-more',        '#9ca3af', 999);
