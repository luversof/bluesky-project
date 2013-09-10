CREATE TABLE `Asset` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '자산 일련번호',
	`username` VARCHAR(255) NULL DEFAULT NULL COMMENT '소유회원 고유이',
	`name` VARCHAR(255) NULL DEFAULT NULL COMMENT '자산이름',
	PRIMARY KEY (`id`),
	INDEX `FK_Asset_username` (`username`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;