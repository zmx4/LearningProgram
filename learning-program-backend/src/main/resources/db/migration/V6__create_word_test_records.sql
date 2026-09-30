CREATE TABLE db_word_test_record
(
    id               INT(11) NOT NULL AUTO_INCREMENT,
    account_id       INT(11) NOT NULL,
    source           VARCHAR(10) NOT NULL,
    total_count      INT NOT NULL,
    correct_count    INT NOT NULL,
    wrong_count      INT NOT NULL,
    score            INT NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_word_test_account FOREIGN KEY (account_id) REFERENCES db_account (id),
    INDEX idx_word_test_account_created (account_id, created_at)
)
    ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
