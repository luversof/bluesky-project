CREATE TABLE `BlogCategory` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '블로그 카테고리 일련번호',
	`username` VARCHAR(255) NULL DEFAULT NULL COMMENT '소유회원 고유이름',
	`name` VARCHAR(255) NULL DEFAULT NULL COMMENT '블로그 카테고리 이름',
	`upperMenu_id` BIGINT(20) NULL DEFAULT NULL COMMENT '상위 블로그 카테고리 일련번호',
	PRIMARY KEY (`id`),
	INDEX `FK_BlogCategory_upperMenu_id` (`upperMenu_id`),
	INDEX `FK_BlogCategory_username` (`username`),
	CONSTRAINT `FK_BlogCategory_upperMenu_id` FOREIGN KEY (`upperMenu_id`) REFERENCES `BlogCategory` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;


CREATE TABLE `Blog` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '블로그 일련번호',
	`username` VARCHAR(255) NULL DEFAULT NULL COMMENT '소유회원 고유이름',
	`title` VARCHAR(255) NULL DEFAULT NULL COMMENT '제목',
	`content` TEXT NULL DEFAULT NULL COMMENT '내용',
	`createdDate` DATETIME NULL DEFAULT NULL COMMENT '생성일시',
	`lastModifiedDate` DATETIME NULL DEFAULT NULL COMMENT '마지막수정일시',
	`blogCategory_id` BIGINT(20) NULL DEFAULT NULL COMMENT '블로그 카테고리 일련번호',
	PRIMARY KEY (`id`),
	INDEX `FK_Blog_blogCategory_id` (`blogCategory_id`),
	INDEX `FK_Blog_username` (`username`),
	CONSTRAINT `FK_Blog_blogCategory_id` FOREIGN KEY (`blogCategory_id`) REFERENCES `BlogCategory` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;
