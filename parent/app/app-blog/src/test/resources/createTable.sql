CREATE TABLE `BlogCategory` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`memberId` BIGINT(20) NOT NULL,
	`title` VARCHAR(255) NULL DEFAULT NULL,
	`upperMenu_id` BIGINT(20) NULL DEFAULT NULL,
	PRIMARY KEY (`id`),
	INDEX `FK_BlogCategory_upperMenu_id` (`upperMenu_id`),
	CONSTRAINT `FK_BlogCategory_upperMenu_id` FOREIGN KEY (`upperMenu_id`) REFERENCES `BlogCategory` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;


CREATE TABLE `BlogPost` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`content` VARCHAR(255) NULL DEFAULT NULL,
	`createdDate` DATETIME NULL DEFAULT NULL,
	`lastModifiedDate` DATETIME NULL DEFAULT NULL,
	`title` VARCHAR(255) NULL DEFAULT NULL,
	`userName` VARCHAR(255) NULL DEFAULT NULL,
	`blogCategory_id` BIGINT(20) NULL DEFAULT NULL,
	PRIMARY KEY (`id`),
	INDEX `FK_BlogPost_blogCategory_id` (`blogCategory_id`),
	CONSTRAINT `FK_BlogPost_blogCategory_id` FOREIGN KEY (`blogCategory_id`) REFERENCES `BlogCategory` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=26;
