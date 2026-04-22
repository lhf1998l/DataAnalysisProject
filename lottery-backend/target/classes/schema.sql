CREATE TABLE IF NOT EXISTS dynamic_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_no INT NOT NULL,
    rule_pattern VARCHAR(64) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dynamic_rule_no (rule_no),
    UNIQUE KEY uk_dynamic_rule_pattern (rule_pattern)
);

CREATE TABLE IF NOT EXISTS dynamic_analysis_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_date VARCHAR(8) NOT NULL,
    rank_no INT NOT NULL,
    dynamic_rule VARCHAR(64) NOT NULL,
    total_miss_count INT NOT NULL DEFAULT 0,
    issue_nos TEXT NOT NULL,
    actual_issue_nos TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dynamic_analysis_source_date_rank_no (source_date, rank_no)
);
