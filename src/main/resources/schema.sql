-- ==========================================================
-- 1. Users Table
-- ==========================================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    avatar VARCHAR(255),
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    phone VARCHAR(50),
    enabled BOOLEAN DEFAULT TRUE,
    locked BOOLEAN DEFAULT FALSE,
    last_login_attempt BIGINT,
    failed_login_attempts INT DEFAULT 0
);

-- ==========================================================
-- 2. Haugfjell MP1 Tables (Header & Axles)
-- ==========================================================

CREATE TABLE IF NOT EXISTS haugfjell_mp1_header (
    train_no       SERIAL PRIMARY KEY,
    mstation       VARCHAR(100) NOT NULL,
    mplace         VARCHAR(100) NOT NULL,
    coo_lat        DOUBLE PRECISION NOT NULL,
    coo_long       DOUBLE PRECISION NOT NULL,
    track_km       INT,
    track_m        INT,
    all_tps_info   TEXT,
    mstart_time    TIMESTAMP,
    mstop_time     TIMESTAMP,
    aversion       VARCHAR(100),
    rversion       VARCHAR(50),
    astart_time    TIMESTAMP,
    astop_time     TIMESTAMP,
    td             VARCHAR(50),
    rfid_devs      VARCHAR(100),
    r_temp         DOUBLE PRECISION,
    a_temp         DOUBLE PRECISION,
    a_press        DOUBLE PRECISION,
    a_hum          DOUBLE PRECISION,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS haugfjell_mp1_axles (
    axle_id       SERIAL PRIMARY KEY,
    train_no      INT NOT NULL,
    ait           VARCHAR(50),
    vty           VARCHAR(50),
    vit           VARCHAR(50),
    aiv           VARCHAR(50),
    fe            VARCHAR(10),
    id_rf2_r      VARCHAR(100),

    -- Speeds
    spd_tp1       DOUBLE PRECISION,
    spd_tp2       DOUBLE PRECISION,
    spd_tp3       DOUBLE PRECISION,
    spd_tp5       DOUBLE PRECISION,
    spd_tp6       DOUBLE PRECISION,
    spd_tp8       DOUBLE PRECISION,

    -- Vertical forces (Left)
    vfrcl_tp1     DOUBLE PRECISION,
    vfrcl_tp2     DOUBLE PRECISION,
    vfrcl_tp3     DOUBLE PRECISION,
    vfrcl_tp5     DOUBLE PRECISION,
    vfrcl_tp6     DOUBLE PRECISION,
    vfrcl_tp8     DOUBLE PRECISION,

    -- Vertical forces (Right)
    vfrcr_tp1     DOUBLE PRECISION,
    vfrcr_tp2     DOUBLE PRECISION,
    vfrcr_tp3     DOUBLE PRECISION,
    vfrcr_tp5     DOUBLE PRECISION,
    vfrcr_tp6     DOUBLE PRECISION,
    vfrcr_tp8     DOUBLE PRECISION,

    -- Angle of Attack
    aoa_tp1       DOUBLE PRECISION,
    aoa_tp2       DOUBLE PRECISION,
    aoa_tp3       DOUBLE PRECISION,
    aoa_tp5       DOUBLE PRECISION,
    aoa_tp6       DOUBLE PRECISION,
    aoa_tp8       DOUBLE PRECISION,

    -- Vertical vibration (Left)
    vvibl_tp1     DOUBLE PRECISION,
    vvibl_tp2     DOUBLE PRECISION,
    vvibl_tp3     DOUBLE PRECISION,
    vvibl_tp5     DOUBLE PRECISION,
    vvibl_tp6     DOUBLE PRECISION,
    vvibl_tp8     DOUBLE PRECISION,

    -- Vertical vibration (Right)
    vvibr_tp1     DOUBLE PRECISION,
    vvibr_tp2     DOUBLE PRECISION,
    vvibr_tp3     DOUBLE PRECISION,
    vvibr_tp5     DOUBLE PRECISION,
    vvibr_tp6     DOUBLE PRECISION,
    vvibr_tp8     DOUBLE PRECISION,

    -- Time delay (Left)
    dtl_tp1       DOUBLE PRECISION,
    dtl_tp2       DOUBLE PRECISION,
    dtl_tp3       DOUBLE PRECISION,
    dtl_tp5       DOUBLE PRECISION,
    dtl_tp6       DOUBLE PRECISION,
    dtl_tp8       DOUBLE PRECISION,

    -- Time delay (Right)
    dtr_tp1       DOUBLE PRECISION,
    dtr_tp2       DOUBLE PRECISION,
    dtr_tp3       DOUBLE PRECISION,
    dtr_tp5       DOUBLE PRECISION,
    dtr_tp6       DOUBLE PRECISION,
    dtr_tp8       DOUBLE PRECISION,

    -- Lateral forces (Left)
    lfrcl_tp1     DOUBLE PRECISION,
    lfrcl_tp2     DOUBLE PRECISION,
    lfrcl_tp3     DOUBLE PRECISION,
    lfrcl_tp5     DOUBLE PRECISION,
    lfrcl_tp6     DOUBLE PRECISION,

    -- Lateral forces (Right)
    lfrcr_tp1     DOUBLE PRECISION,
    lfrcr_tp2     DOUBLE PRECISION,
    lfrcr_tp3     DOUBLE PRECISION,
    lfrcr_tp5     DOUBLE PRECISION,
    lfrcr_tp6     DOUBLE PRECISION,

    -- Lateral vibration (Left)
    lvibl_tp1     DOUBLE PRECISION,
    lvibl_tp2     DOUBLE PRECISION,
    lvibl_tp3     DOUBLE PRECISION,
    lvibl_tp5     DOUBLE PRECISION,
    lvibl_tp6     DOUBLE PRECISION,

    -- Lateral vibration (Right)
    lvibr_tp1     DOUBLE PRECISION,
    lvibr_tp2     DOUBLE PRECISION,
    lvibr_tp3     DOUBLE PRECISION,
    lvibr_tp5     DOUBLE PRECISION,
    lvibr_tp6     DOUBLE PRECISION,

    -- Longitudinal
    lngl_tp1      DOUBLE PRECISION,
    lngl_tp8      DOUBLE PRECISION,
    lngr_tp1      DOUBLE PRECISION,
    lngr_tp8      DOUBLE PRECISION,

    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    segment_id    INT,

    FOREIGN KEY (train_no) REFERENCES haugfjell_mp1_header(train_no)
);

-- [INDEXES]
CREATE INDEX IF NOT EXISTS idx_mp1_axles_train_created
    ON haugfjell_mp1_axles(train_no, created_at);

-- ==========================================================
-- 3. Haugfjell MP3 Tables (Header & Axles)
-- ==========================================================

CREATE TABLE IF NOT EXISTS haugfjell_mp3_header (
    train_no       SERIAL PRIMARY KEY,
    mstation       VARCHAR(100) NOT NULL,
    mplace         VARCHAR(100) NOT NULL,
    coo_lat        DOUBLE PRECISION NOT NULL,
    coo_long       DOUBLE PRECISION NOT NULL,
    track_km       INT,
    track_m        INT,
    all_tps_info   TEXT,
    mstart_time    TIMESTAMP,
    mstop_time     TIMESTAMP,
    aversion       VARCHAR(100),
    rversion       VARCHAR(50),
    astart_time    TIMESTAMP,
    astop_time     TIMESTAMP,
    td             VARCHAR(50),
    rfid_devs      VARCHAR(100),
    r_temp         DOUBLE PRECISION,
    a_temp         DOUBLE PRECISION,
    a_press        DOUBLE PRECISION,
    a_hum          DOUBLE PRECISION,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS haugfjell_mp3_axles (
    axle_id       SERIAL PRIMARY KEY,
    train_no      INT NOT NULL,
    ait           VARCHAR(50),
    vty           VARCHAR(50),
    vit           VARCHAR(50),
    aiv           VARCHAR(50),
    fe            VARCHAR(10),
    id_rf2_r      VARCHAR(100),

    spd_tp1       DOUBLE PRECISION,
    spd_tp2       DOUBLE PRECISION,
    spd_tp3       DOUBLE PRECISION,
    spd_tp5       DOUBLE PRECISION,
    spd_tp6       DOUBLE PRECISION,
    spd_tp8       DOUBLE PRECISION,

    vfrcl_tp1     DOUBLE PRECISION,
    vfrcl_tp2     DOUBLE PRECISION,
    vfrcl_tp3     DOUBLE PRECISION,
    vfrcl_tp5     DOUBLE PRECISION,
    vfrcl_tp6     DOUBLE PRECISION,
    vfrcl_tp8     DOUBLE PRECISION,

    vfrcr_tp1     DOUBLE PRECISION,
    vfrcr_tp2     DOUBLE PRECISION,
    vfrcr_tp3     DOUBLE PRECISION,
    vfrcr_tp5     DOUBLE PRECISION,
    vfrcr_tp6     DOUBLE PRECISION,
    vfrcr_tp8     DOUBLE PRECISION,

    aoa_tp1       DOUBLE PRECISION,
    aoa_tp2       DOUBLE PRECISION,
    aoa_tp3       DOUBLE PRECISION,
    aoa_tp5       DOUBLE PRECISION,
    aoa_tp6       DOUBLE PRECISION,
    aoa_tp8       DOUBLE PRECISION,

    vvibl_tp1     DOUBLE PRECISION,
    vvibl_tp2     DOUBLE PRECISION,
    vvibl_tp3     DOUBLE PRECISION,
    vvibl_tp5     DOUBLE PRECISION,
    vvibl_tp6     DOUBLE PRECISION,
    vvibl_tp8     DOUBLE PRECISION,

    vvibr_tp1     DOUBLE PRECISION,
    vvibr_tp2     DOUBLE PRECISION,
    vvibr_tp3     DOUBLE PRECISION,
    vvibr_tp5     DOUBLE PRECISION,
    vvibr_tp6     DOUBLE PRECISION,
    vvibr_tp8     DOUBLE PRECISION,

    dtl_tp1       DOUBLE PRECISION,
    dtl_tp2       DOUBLE PRECISION,
    dtl_tp3       DOUBLE PRECISION,
    dtl_tp5       DOUBLE PRECISION,
    dtl_tp6       DOUBLE PRECISION,
    dtl_tp8       DOUBLE PRECISION,

    dtr_tp1       DOUBLE PRECISION,
    dtr_tp2       DOUBLE PRECISION,
    dtr_tp3       DOUBLE PRECISION,
    dtr_tp5       DOUBLE PRECISION,
    dtr_tp6       DOUBLE PRECISION,
    dtr_tp8       DOUBLE PRECISION,

    lfrcl_tp1     DOUBLE PRECISION,
    lfrcl_tp2     DOUBLE PRECISION,
    lfrcl_tp3     DOUBLE PRECISION,
    lfrcl_tp5     DOUBLE PRECISION,
    lfrcl_tp6     DOUBLE PRECISION,

    lfrcr_tp1     DOUBLE PRECISION,
    lfrcr_tp2     DOUBLE PRECISION,
    lfrcr_tp3     DOUBLE PRECISION,
    lfrcr_tp5     DOUBLE PRECISION,
    lfrcr_tp6     DOUBLE PRECISION,

    lvibl_tp1     DOUBLE PRECISION,
    lvibl_tp2     DOUBLE PRECISION,
    lvibl_tp3     DOUBLE PRECISION,
    lvibl_tp5     DOUBLE PRECISION,
    lvibl_tp6     DOUBLE PRECISION,

    lvibr_tp1     DOUBLE PRECISION,
    lvibr_tp2     DOUBLE PRECISION,
    lvibr_tp3     DOUBLE PRECISION,
    lvibr_tp5     DOUBLE PRECISION,
    lvibr_tp6     DOUBLE PRECISION,

    lngl_tp1      DOUBLE PRECISION,
    lngl_tp8      DOUBLE PRECISION,
    lngr_tp1      DOUBLE PRECISION,
    lngr_tp8      DOUBLE PRECISION,

    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    segment_id    INT,

    FOREIGN KEY (train_no) REFERENCES haugfjell_mp3_header(train_no)
);

-- [INDEXES]
CREATE INDEX IF NOT EXISTS idx_mp3_axles_train_created
    ON haugfjell_mp3_axles(train_no, created_at);

-- ==========================================================
-- 4. Station Synchronization Table (Optional)
-- ==========================================================
CREATE TABLE IF NOT EXISTS station_synchronization (
    id             SERIAL PRIMARY KEY,
    station        VARCHAR(50) NOT NULL,
    last_train_no  INT NOT NULL,
    last_sync_time TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================================
-- 5. Table: user_settings
-- Consolidates general, dashboard, notification & security
-- ==========================================================
DROP TABLE IF EXISTS user_settings;

CREATE TABLE user_settings (
    user_id              BIGINT      PRIMARY KEY
      REFERENCES users(user_id)
      ON DELETE CASCADE,
    -- General Settings
    username             VARCHAR(100)    NOT NULL,
    email                VARCHAR(255)    NOT NULL,
    avatar_url           VARCHAR(512),
    language             VARCHAR(50)     NOT NULL DEFAULT 'en',
    timezone             VARCHAR(50)     NOT NULL DEFAULT 'UTC',
    date_format          VARCHAR(50)     NOT NULL DEFAULT 'yyyy-MM-dd',
    theme                VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM',
    -- Dashboard Widget Settings
    show_speed_widget        BOOLEAN    NOT NULL DEFAULT TRUE,
    show_fuel_widget         BOOLEAN    NOT NULL DEFAULT TRUE,
    show_performance_widget  BOOLEAN    NOT NULL DEFAULT TRUE,
    -- Notification Settings
    enable_notifications     BOOLEAN    NOT NULL DEFAULT TRUE,
    email_alerts             BOOLEAN    NOT NULL DEFAULT TRUE,
    sms_alerts               BOOLEAN    NOT NULL DEFAULT FALSE,
    -- Security Settings
    two_factor_enabled       BOOLEAN    NOT NULL DEFAULT FALSE,
    phone_number             VARCHAR(20),
    -- Timestamps
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ==========================================================
-- 6. Alert History
-- ==========================================================

CREATE TABLE IF NOT EXISTS alert_history (
    id               SERIAL        PRIMARY KEY,
    subject          VARCHAR(255)  NOT NULL,
    message          TEXT          NOT NULL,
    severity         VARCHAR(50),       -- e.g. INFO, WARN, ERROR
    train_no         INTEGER,       -- corresponds to AlertDTO.trainNo
    timestamp        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged     BOOLEAN       NOT NULL DEFAULT FALSE,
    acknowledged_by  VARCHAR(255)                    -- who ack’d the alert
);

-- Index to get most recent alerts quickly
CREATE INDEX IF NOT EXISTS idx_alert_history_timestamp
    ON alert_history(timestamp DESC);

-- Index to filter by train number efficiently
CREATE INDEX IF NOT EXISTS idx_alert_history_train_no
    ON alert_history(train_no);

-- ==========================================================
-- 7. Digital Twins
-- ==========================================================
CREATE TABLE IF NOT EXISTS digital_twins (
    id               BIGSERIAL PRIMARY KEY,
    asset_id         INTEGER   NOT NULL,
    recorded_at      TIMESTAMP NOT NULL,
    metric_value     DOUBLE PRECISION NOT NULL,
    metric_type      VARCHAR(100)  NOT NULL,
    component_name   VARCHAR(100)  NOT NULL,
    location         VARCHAR(200),
    status           VARCHAR(100),
    risk_score       DOUBLE PRECISION,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dtm_asset_time
  ON digital_twins(asset_id, recorded_at DESC);
-- ==========================================================
-- 8. Verification token
-- ==========================================================
CREATE TABLE IF NOT EXISTS verification_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
