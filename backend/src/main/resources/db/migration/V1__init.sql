-- 习惯打卡应用初始化迁移
-- 所有业务时间戳统一按 UTC 写入，业务日期由应用层按 Asia/Shanghai 计算并落为 DATE。

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(60)  NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE habits (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- 支撑复合外键 (user_id, id) 与按归属快速查询
    CONSTRAINT uk_habits_user_id_id UNIQUE (user_id, id),
    CONSTRAINT fk_habits_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE check_in_records (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    habit_id      BIGINT       NOT NULL,
    check_in_date DATE         NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- 并发幂等的核心：同一用户、同一打卡项、同一自然日仅一条
    CONSTRAINT uk_records_user_habit_date UNIQUE (user_id, habit_id, check_in_date),
    KEY idx_records_user_date (user_id, check_in_date),
    -- 从数据库层阻止记录挂到他人打卡项
    CONSTRAINT fk_records_habit FOREIGN KEY (user_id, habit_id)
        REFERENCES habits (user_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 打卡项新增可选目标时长（分钟）。NULL 表示未设置，取值区间由应用层校验为 1-1440。
ALTER TABLE habits ADD COLUMN duration_minutes INT NULL;
