CREATE TABLE db_dictionary
(
    id          INT(11)      NOT NULL AUTO_INCREMENT,
    word        VARCHAR(255) NOT NULL,
    translation TEXT         DEFAULT NULL,
    PRIMARY KEY (id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 3402565
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
