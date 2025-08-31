CREATE TABLE `Board` (
	`id` UUID NOT NULL,
	`alias` VARCHAR(15) NOT NULL,
	`bitConfig` VARBINARY(255) NULL DEFAULT NULL,
	`jsonConfig` LONGTEXT NULL DEFAULT NULL,
	PRIMARY KEY (`id`),
	UNIQUE INDEX `UK_board_alias` (`alias`),
	CONSTRAINT `jsonConfig` CHECK (json_valid(`jsonConfig`))
);

CREATE TABLE `BoardArticle` (
	`id` UUID NOT NULL,
	`board_id` UUID NOT NULL,
	`content` VARCHAR(255) NULL DEFAULT NULL,
	`createdDate` DATETIME(6) NULL DEFAULT NULL,
	`lastModifiedDate` DATETIME(6) NULL DEFAULT NULL,
	`title` VARCHAR(255) NULL DEFAULT NULL,
	`user_id` VARCHAR(36) NOT NULL,
	PRIMARY KEY (`id`),
	INDEX `IDX_article_boardId` (`board_id`),
	INDEX `IDX_article_userId` (`user_id`)
);
