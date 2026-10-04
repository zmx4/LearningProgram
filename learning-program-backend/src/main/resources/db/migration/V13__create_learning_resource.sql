-- 学习资源：管理员上传的资料文件，文件本体存放在 learning.resources.upload-dir 配置的目录。
-- stored_name 为磁盘托管文件名（UUID.扩展名），file_name 保留上传时的原始文件名用于展示与下载命名；
-- visible 决定是否出现在学员端学习资源页面，隐藏的资源仅管理员可见可下载。
CREATE TABLE db_learning_resource
(
    id           INT(11)      NOT NULL AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(255) DEFAULT NULL,
    file_name    VARCHAR(255) NOT NULL,
    stored_name  VARCHAR(100) NOT NULL,
    content_type VARCHAR(100) DEFAULT NULL,
    file_size    BIGINT       NOT NULL DEFAULT 0,
    visible      TINYINT(1)   NOT NULL DEFAULT 1,
    account_id   INT(11)      DEFAULT NULL COMMENT '上传者，软引用 db_account，不带外键',
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_learning_resource_visible (visible, created_at)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
