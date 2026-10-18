-- AI Weather & Heavy Rain Prediction System - core schema (MySQL 8, InnoDB, utf8mb4)

CREATE TABLE locations (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120)  NOT NULL,
    admin1        VARCHAR(120)  NULL COMMENT 'state / region as returned by the geocoder',
    admin2        VARCHAR(120)  NULL COMMENT 'district / county as returned by the geocoder',
    country_code  CHAR(2)       NULL,
    latitude      DECIMAL(8,5)  NOT NULL,
    longitude     DECIMAL(8,5)  NOT NULL,
    timezone      VARCHAR(64)   NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_locations_coord (latitude, longitude),
    CONSTRAINT chk_locations_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_locations_lon CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE weather_forecast_snapshots (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    location_id         BIGINT UNSIGNED NOT NULL,
    provider            VARCHAR(40)  NOT NULL,
    fetched_at          TIMESTAMP    NOT NULL,
    forecast_start_date DATE         NOT NULL,
    forecast_end_date   DATE         NOT NULL,
    timezone            VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_snapshots_location_fetched (location_id, fetched_at),
    KEY idx_snapshots_fetched (fetched_at),
    CONSTRAINT fk_snapshots_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE weather_forecast_daily (
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    snapshot_id                 BIGINT UNSIGNED NOT NULL,
    forecast_date               DATE          NOT NULL,
    precipitation_sum_mm        DECIMAL(7,2)  NULL,
    precipitation_probability_max TINYINT UNSIGNED NULL COMMENT 'percent 0-100',
    temperature_max_c           DECIMAL(5,2)  NULL,
    temperature_min_c           DECIMAL(5,2)  NULL,
    wind_speed_max_kmh          DECIMAL(6,2)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_daily_snapshot_date (snapshot_id, forecast_date),
    CONSTRAINT fk_daily_snapshot FOREIGN KEY (snapshot_id) REFERENCES weather_forecast_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT chk_daily_prob CHECK (precipitation_probability_max IS NULL OR precipitation_probability_max <= 100),
    CONSTRAINT chk_daily_precip CHECK (precipitation_sum_mm IS NULL OR precipitation_sum_mm >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE risk_assessments (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    location_id             BIGINT UNSIGNED NOT NULL,
    snapshot_id             BIGINT UNSIGNED NULL,
    method                  VARCHAR(40)  NOT NULL COMMENT 'RULE_BASED',
    method_version          VARCHAR(40)  NOT NULL,
    category                ENUM('LOW','MODERATE','HIGH','VERY_HIGH','INSUFFICIENT_DATA') NOT NULL,
    window_start            DATE         NOT NULL,
    window_end              DATE         NOT NULL,
    max_daily_precip_mm     DECIMAL(7,2) NULL,
    max_hourly_precip_mm    DECIMAL(7,2) NULL,
    max_precip_probability  TINYINT UNSIGNED NULL,
    reasons_json            JSON         NOT NULL,
    thresholds_json         JSON         NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_risk_location_created (location_id, created_at),
    KEY idx_risk_category (category),
    CONSTRAINT fk_risk_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE CASCADE,
    CONSTRAINT fk_risk_snapshot FOREIGN KEY (snapshot_id) REFERENCES weather_forecast_snapshots (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE weather_alerts (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    origin        ENUM('ADMIN_ENTERED_OFFICIAL','SYSTEM_GENERATED') NOT NULL
                  COMMENT 'ADMIN_ENTERED_OFFICIAL = typed in by an administrator from an official source; NOT auto-verified',
    title         VARCHAR(160)  NOT NULL,
    message       TEXT          NOT NULL,
    severity      ENUM('INFO','YELLOW','ORANGE','RED') NOT NULL,
    source_name   VARCHAR(120)  NOT NULL,
    source_url    VARCHAR(500)  NOT NULL,
    issued_at     TIMESTAMP     NOT NULL,
    valid_from    TIMESTAMP     NOT NULL,
    valid_to      TIMESTAMP     NOT NULL,
    affected_areas VARCHAR(500) NOT NULL COMMENT 'free text, e.g. district names',
    center_latitude  DECIMAL(8,5) NULL,
    center_longitude DECIMAL(8,5) NULL,
    radius_km        DECIMAL(7,2) NULL,
    is_test       BOOLEAN       NOT NULL DEFAULT FALSE,
    dedupe_key    CHAR(64)      NOT NULL COMMENT 'sha-256 of origin|source_url|areas|valid_from|valid_to',
    created_by    VARCHAR(80)   NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_alert_dedupe (dedupe_key),
    KEY idx_alert_validity (valid_from, valid_to),
    KEY idx_alert_severity (severity),
    CONSTRAINT chk_alert_period CHECK (valid_to > valid_from),
    CONSTRAINT chk_alert_geo CHECK (
        (center_latitude IS NULL AND center_longitude IS NULL AND radius_km IS NULL)
        OR (center_latitude IS NOT NULL AND center_longitude IS NOT NULL AND radius_km IS NOT NULL AND radius_km > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
