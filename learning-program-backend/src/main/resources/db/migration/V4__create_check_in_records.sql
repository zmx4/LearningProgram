ALTER TABLE db_account ADD COLUMN points INT NOT NULL DEFAULT 0;

CREATE TABLE db_check_in
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    account_id INT(11) NOT NULL,
    checkin_date DATE NOT NULL,
    points INT NOT NULL,
    streak INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_check_in_account_date (account_id, checkin_date),
    CONSTRAINT fk_check_in_account FOREIGN KEY (account_id) REFERENCES db_account (id)
)
ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
