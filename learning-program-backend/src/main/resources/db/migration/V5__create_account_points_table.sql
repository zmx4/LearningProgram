CREATE TABLE db_account_points
(
    id           INT(11) NOT NULL AUTO_INCREMENT,
    account_id   INT(11) NOT NULL,
    total_points INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_points_account (account_id),
    CONSTRAINT fk_account_points_account FOREIGN KEY (account_id) REFERENCES db_account (id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

INSERT INTO db_account_points (account_id, total_points)
SELECT id, points
FROM db_account
WHERE points > 0;
