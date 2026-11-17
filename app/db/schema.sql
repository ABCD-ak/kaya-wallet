-- =============================================================
-- Kaya Wallet 数据库结构
-- DB: MySQL 8.0.16+ / InnoDB / utf8mb4
-- 说明：所有金额为 DECIMAL(18,2)，时间统一存 UTC，应用层做时区转换
-- 初始化前按需执行：
--   CREATE DATABASE IF NOT EXISTS kaya
--     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--   USE kaya;
-- =============================================================

SET NAMES utf8mb4;

-- -------------------------------------------------------------
-- 1. 用户
-- -------------------------------------------------------------
CREATE TABLE users (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  email            VARCHAR(254)    NOT NULL                COMMENT '登录邮箱',
  phone            VARCHAR(32)     NULL                    COMMENT '手机号（E.164）',
  password_hash    VARCHAR(255)    NOT NULL                COMMENT 'Argon2id / bcrypt 哈希，禁止明文',
  display_name     VARCHAR(64)     NOT NULL                COMMENT '展示名称',
  username         VARCHAR(32)     NOT NULL                COMMENT '唯一用户名',
  avatar_url       VARCHAR(512)    NULL                    COMMENT '头像地址',
  gender           VARCHAR(16)     NULL                    COMMENT 'male/female/other',
  birthday         DATE            NULL                    COMMENT '生日',
  email_verified_at DATETIME(3)    NULL                    COMMENT '邮箱验证时间',
  phone_verified_at DATETIME(3)    NULL                    COMMENT '手机号验证时间',
  status           VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active/disabled/locked',
  last_login_at    DATETIME(3)     NULL                    COMMENT '最近登录时间',
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at       DATETIME(3)     NULL                    COMMENT '软删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_phone (phone),
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_status (status),
  KEY idx_users_deleted_at (deleted_at),
  CONSTRAINT chk_users_status CHECK (status IN ('active', 'disabled', 'locked')),
  CONSTRAINT chk_users_gender CHECK (gender IS NULL OR gender IN ('male', 'female', 'other'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户账号';

-- -------------------------------------------------------------
-- 2. 用户偏好设置
-- -------------------------------------------------------------
CREATE TABLE user_settings (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id              BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  locale               VARCHAR(16)     NOT NULL DEFAULT 'zh-CN' COMMENT '语言',
  timezone             VARCHAR(64)     NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
  default_currency_code CHAR(3)        NOT NULL DEFAULT 'USD'  COMMENT '默认币种（ISO 4217）',
  push_enabled         TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '推送通知开关',
  email_enabled        TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '邮件通知开关',
  sms_enabled          TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '短信通知开关',
  marketing_enabled    TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '营销通知开关',
  created_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_settings_user_id (user_id),
  CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户偏好设置';

-- -------------------------------------------------------------
-- 3. 收货 / 联系地址
-- -------------------------------------------------------------
CREATE TABLE user_addresses (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id          BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  label            VARCHAR(32)     NULL                    COMMENT '标签：家/公司',
  recipient_name   VARCHAR(64)     NOT NULL                COMMENT '收件人',
  phone            VARCHAR(32)     NOT NULL                COMMENT '联系电话',
  country_code     CHAR(2)         NOT NULL DEFAULT 'ID'   COMMENT '国家代码（ISO 3166-1）',
  province         VARCHAR(64)     NULL                    COMMENT '省/州',
  city             VARCHAR(64)     NULL                    COMMENT '城市',
  district         VARCHAR(64)     NULL                    COMMENT '区县',
  detail_address   VARCHAR(255)    NOT NULL                COMMENT '详细地址',
  postal_code      VARCHAR(16)     NULL                    COMMENT '邮编',
  is_default       TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否默认地址',
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at       DATETIME(3)     NULL                    COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_user_addresses_user_default (user_id, is_default),
  CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户地址';

-- -------------------------------------------------------------
-- 4. 实名 / 联系方式认证
-- -------------------------------------------------------------
CREATE TABLE user_verifications (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id               BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  type                  VARCHAR(16)     NOT NULL                COMMENT 'identity/address/phone/email',
  status                VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/verified/rejected',
  document_type         VARCHAR(32)     NULL                    COMMENT '证件类型',
  document_no_encrypted VARBINARY(1024) NULL                    COMMENT '证件号密文（应用层加密）',
  document_no_last4     CHAR(4)         NULL                    COMMENT '证件号后四位（脱敏展示）',
  verified_at           DATETIME(3)     NULL                    COMMENT '认证完成时间',
  rejected_reason       VARCHAR(255)    NULL                    COMMENT '驳回原因',
  created_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_verifications_user_type (user_id, type),
  CONSTRAINT fk_user_verifications_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_user_verifications_type CHECK (type IN ('identity', 'address', 'phone', 'email')),
  CONSTRAINT chk_user_verifications_status CHECK (status IN ('pending', 'verified', 'rejected'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户认证信息';

-- -------------------------------------------------------------
-- 5. 钱包（每个用户每种币种一个）
-- -------------------------------------------------------------
CREATE TABLE wallets (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  currency_code CHAR(3)         NOT NULL DEFAULT 'USD'  COMMENT '币种（ISO 4217）',
  balance       DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '当前余额（缓存值，与流水同事务更新）',
  status        VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active/frozen/closed',
  version       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallets_user_currency (user_id, currency_code),
  CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_wallets_status CHECK (status IN ('active', 'frozen', 'closed')),
  CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户钱包';

-- -------------------------------------------------------------
-- 6. 银行卡 / 信用卡
--    禁止存储完整卡号、CVV；支付信息只存网关 token + 后四位
-- -------------------------------------------------------------
CREATE TABLE wallet_cards (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id            BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  wallet_id          BIGINT UNSIGNED NULL                    COMMENT '关联钱包 ID',
  card_type          VARCHAR(16)     NOT NULL                COMMENT 'debit/credit',
  brand              VARCHAR(16)     NOT NULL                COMMENT 'visa/mastercard/unionpay',
  card_number_last4  CHAR(4)         NOT NULL                COMMENT '卡号后四位',
  holder_name        VARCHAR(64)     NOT NULL                COMMENT '持卡人姓名',
  expiry_month       TINYINT UNSIGNED NOT NULL               COMMENT '有效期月份 1-12',
  expiry_year        SMALLINT UNSIGNED NOT NULL              COMMENT '有效期年份',
  currency_code      CHAR(3)         NOT NULL DEFAULT 'USD'  COMMENT '币种',
  balance            DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '卡内余额 / 已用额度',
  credit_limit       DECIMAL(18,2)   NULL                    COMMENT '信用额度（信用卡）',
  payment_token      VARCHAR(191)    NULL                    COMMENT '支付网关 token',
  is_primary         TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否主卡',
  status             VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active/expired/frozen/removed',
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at         DATETIME(3)     NULL                    COMMENT '软删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_cards_payment_token (payment_token),
  KEY idx_wallet_cards_user_status (user_id, status),
  KEY idx_wallet_cards_wallet (wallet_id),
  CONSTRAINT fk_wallet_cards_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_wallet_cards_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT chk_wallet_cards_type CHECK (card_type IN ('debit', 'credit')),
  CONSTRAINT chk_wallet_cards_status CHECK (status IN ('active', 'expired', 'frozen', 'removed')),
  CONSTRAINT chk_wallet_cards_expiry_month CHECK (expiry_month BETWEEN 1 AND 12),
  CONSTRAINT chk_wallet_cards_expiry_year CHECK (expiry_year >= 2000),
  CONSTRAINT chk_wallet_cards_balance CHECK (balance >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户银行卡';

-- -------------------------------------------------------------
-- 7. 交易分类（系统预置；如需用户自定义，可扩展 user_id 并调整唯一键）
-- -------------------------------------------------------------
CREATE TABLE categories (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code       VARCHAR(32)     NOT NULL                COMMENT '稳定业务编码',
  type       VARCHAR(16)     NOT NULL                COMMENT 'income/expense',
  name       VARCHAR(64)     NOT NULL                COMMENT '展示名称',
  icon       VARCHAR(64)     NULL                    COMMENT '图标标识',
  color      CHAR(7)         NULL                    COMMENT '主题色 #RRGGBB',
  sort_order INT             NOT NULL DEFAULT 0      COMMENT '排序',
  is_active  TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否启用',
  created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_code (code),
  KEY idx_categories_type_active (type, is_active, sort_order),
  CONSTRAINT chk_categories_type CHECK (type IN ('income', 'expense'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易分类';

-- -------------------------------------------------------------
-- 8. 预算（按用户 + 分类 + 周期）
-- -------------------------------------------------------------
CREATE TABLE budgets (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id         BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  category_id     BIGINT UNSIGNED NOT NULL                COMMENT '分类 ID',
  period_type     VARCHAR(16)     NOT NULL DEFAULT 'monthly' COMMENT 'monthly/weekly/custom',
  period_start    DATE            NOT NULL                COMMENT '周期开始日期（含）',
  period_end      DATE            NOT NULL                COMMENT '周期结束日期（含）',
  amount_limit    DECIMAL(18,2)   NOT NULL                COMMENT '预算金额',
  alert_threshold DECIMAL(5,2)    NOT NULL DEFAULT 80.00  COMMENT '提醒阈值（百分比）',
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_budgets_user_category_period (user_id, category_id, period_start),
  KEY idx_budgets_user_period (user_id, period_start, period_end),
  KEY idx_budgets_category (category_id),
  CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_budgets_period_type CHECK (period_type IN ('monthly', 'weekly', 'custom')),
  CONSTRAINT chk_budgets_period CHECK (period_end >= period_start),
  CONSTRAINT chk_budgets_amount CHECK (amount_limit > 0),
  CONSTRAINT chk_budgets_threshold CHECK (alert_threshold >= 0 AND alert_threshold <= 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户预算';

-- -------------------------------------------------------------
-- 9. 收款人 / 联系人
-- -------------------------------------------------------------
CREATE TABLE recipients (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT UNSIGNED NOT NULL                COMMENT '所属用户 ID',
  display_name  VARCHAR(64)     NOT NULL                COMMENT '收款人姓名',
  phone         VARCHAR(32)     NULL                    COMMENT '手机号',
  email         VARCHAR(254)    NULL                    COMMENT '邮箱',
  account_no    VARCHAR(64)     NULL                    COMMENT '收款账号',
  bank_name     VARCHAR(64)     NULL                    COMMENT '银行名称',
  avatar_url    VARCHAR(512)    NULL                    COMMENT '头像地址',
  is_favorite   TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否常用',
  last_used_at  DATETIME(3)     NULL                    COMMENT '最近转账时间',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at    DATETIME(3)     NULL                    COMMENT '软删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_recipients_user_account (user_id, account_no),
  KEY idx_recipients_user_last_used (user_id, last_used_at DESC),
  CONSTRAINT fk_recipients_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收款联系人';

-- -------------------------------------------------------------
-- 10. 交易流水（单边记账：一条记录对应一个钱包的一次资金变动）
--     direction 决定入账/出账；transfer_out 与 transfer_in 可通过
--     related_transaction_id 关联；金额始终为正数
-- -------------------------------------------------------------
CREATE TABLE transactions (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  reference_no             VARCHAR(32)     NOT NULL                COMMENT '对外交易号（建议 ULID）',
  user_id                  BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  wallet_id                BIGINT UNSIGNED NOT NULL                COMMENT '钱包 ID',
  type                     VARCHAR(16)     NOT NULL                COMMENT 'income/expense/transfer_out/transfer_in/refund',
  direction                VARCHAR(8)      NOT NULL                COMMENT 'credit 入账 / debit 出账',
  status                   VARCHAR(16)     NOT NULL DEFAULT 'success' COMMENT 'pending/success/failed/cancelled',
  amount                   DECIMAL(18,2)   NOT NULL                COMMENT '交易金额（正数）',
  fee_amount               DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '手续费（正数）',
  currency_code            CHAR(3)         NOT NULL                COMMENT '币种',
  category_id              BIGINT UNSIGNED NULL                    COMMENT '分类 ID（收入/支出必填）',
  counterparty_user_id     BIGINT UNSIGNED NULL                    COMMENT '对方用户 ID（站内转账）',
  recipient_id             BIGINT UNSIGNED NULL                    COMMENT '收款联系人 ID',
  counterparty_name        VARCHAR(128)    NULL                    COMMENT '对方名称快照',
  counterparty_account_no  VARCHAR(64)     NULL                    COMMENT '对方账号快照',
  related_transaction_id   BIGINT UNSIGNED NULL                    COMMENT '关联流水（转账双边/退款）',
  idempotency_key          VARCHAR(64)     NULL                    COMMENT '幂等键，防止重复扣款',
  note                     VARCHAR(255)    NULL                    COMMENT '备注',
  occurred_at              DATETIME(3)     NOT NULL                COMMENT '业务发生时间（UTC）',
  created_at               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_transactions_reference_no (reference_no),
  UNIQUE KEY uk_transactions_idempotency_key (idempotency_key),
  KEY idx_transactions_wallet_time (wallet_id, occurred_at DESC, id DESC),
  KEY idx_transactions_user_time (user_id, occurred_at DESC, id DESC),
  KEY idx_transactions_category_time (category_id, occurred_at),
  KEY idx_transactions_counterparty_user (counterparty_user_id),
  KEY idx_transactions_recipient (recipient_id),
  KEY idx_transactions_related (related_transaction_id),
  KEY idx_transactions_type_status (type, status),
  CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_transactions_counterparty_user FOREIGN KEY (counterparty_user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_transactions_recipient FOREIGN KEY (recipient_id) REFERENCES recipients (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_transactions_related FOREIGN KEY (related_transaction_id) REFERENCES transactions (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT chk_transactions_type CHECK (type IN ('income', 'expense', 'transfer_out', 'transfer_in', 'refund')),
  CONSTRAINT chk_transactions_direction CHECK (direction IN ('credit', 'debit')),
  CONSTRAINT chk_transactions_status CHECK (status IN ('pending', 'success', 'failed', 'cancelled')),
  CONSTRAINT chk_transactions_amount CHECK (amount > 0),
  CONSTRAINT chk_transactions_fee CHECK (fee_amount >= 0),
  CONSTRAINT chk_transactions_category CHECK (
    type NOT IN ('income', 'expense') OR category_id IS NOT NULL
  )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易流水';

-- -------------------------------------------------------------
-- 11. 消息通知
-- -------------------------------------------------------------
CREATE TABLE notifications (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id                BIGINT UNSIGNED NOT NULL                COMMENT '接收用户 ID',
  type                   VARCHAR(32)     NOT NULL                COMMENT 'transfer_received/budget_alert/payment_received/income/system',
  title                  VARCHAR(128)    NOT NULL                COMMENT '标题',
  body                   VARCHAR(512)    NOT NULL                COMMENT '内容',
  amount                 DECIMAL(18,2)   NULL                    COMMENT '关联金额',
  currency_code          CHAR(3)         NULL                    COMMENT '关联币种',
  related_transaction_id BIGINT UNSIGNED NULL                    COMMENT '关联交易 ID',
  is_read                TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否已读',
  read_at                DATETIME(3)     NULL                    COMMENT '阅读时间',
  created_at             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notifications_user_read_time (user_id, is_read, created_at DESC),
  KEY idx_notifications_related_transaction (related_transaction_id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_notifications_transaction FOREIGN KEY (related_transaction_id) REFERENCES transactions (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT chk_notifications_amount CHECK (amount IS NULL OR amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息通知';

-- -------------------------------------------------------------
-- 12. 登录会话（刷新令牌只存哈希）
-- -------------------------------------------------------------
CREATE TABLE user_sessions (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id            BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  refresh_token_hash CHAR(64)        NOT NULL                COMMENT '刷新令牌 SHA-256（hex）',
  user_agent         VARCHAR(255)    NULL                    COMMENT '客户端 UA',
  ip_address         VARCHAR(45)     NULL                    COMMENT '客户端 IP（兼容 IPv6）',
  expires_at         DATETIME(3)     NOT NULL                COMMENT '过期时间',
  revoked_at         DATETIME(3)     NULL                    COMMENT '吊销时间',
  last_used_at       DATETIME(3)     NULL                    COMMENT '最近使用时间',
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_sessions_token (refresh_token_hash),
  KEY idx_user_sessions_user_expires (user_id, expires_at),
  CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户登录会话';

-- -------------------------------------------------------------
-- 13. 找回密码令牌
-- -------------------------------------------------------------
CREATE TABLE password_reset_tokens (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT UNSIGNED NOT NULL                COMMENT '用户 ID',
  token_hash CHAR(64)        NOT NULL                COMMENT '令牌 SHA-256（hex）',
  expires_at DATETIME(3)     NOT NULL                COMMENT '过期时间',
  used_at    DATETIME(3)     NULL                    COMMENT '使用时间',
  created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_tokens_hash (token_hash),
  KEY idx_password_reset_tokens_user (user_id),
  CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '找回密码令牌';

-- 邮箱验证码（登录 / 注册 / 找回密码）
CREATE TABLE email_codes (
  id         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
  email      VARCHAR(254)     NOT NULL                COMMENT '收件邮箱（统一小写）',
  code_hash  CHAR(64)         NOT NULL                COMMENT '验证码 SHA-256（hex）',
  purpose    VARCHAR(16)      NOT NULL DEFAULT 'login' COMMENT '用途：login/register/reset',
  expires_at DATETIME(3)      NOT NULL                COMMENT '过期时间',
  used_at    DATETIME(3)      NULL                    COMMENT '使用时间',
  attempts   TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '错误次数',
  ip_address VARCHAR(45)      NULL                    COMMENT '请求 IP',
  created_at DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_email_codes_email_purpose (email, purpose, created_at),
  KEY idx_email_codes_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邮箱验证码';
