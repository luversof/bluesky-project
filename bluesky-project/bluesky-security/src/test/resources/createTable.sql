CREATE TABLE User (
	id BIGINT(20) NOT NULL AUTO_INCREMENT,
	username VARCHAR(255) NULL DEFAULT NULL,
	password VARCHAR(255) NULL DEFAULT NULL,
	enable TINYINT NULL DEFAULT NULL,
	PRIMARY KEY (id),
	INDEX FK_User_username (username)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;


CREATE TABLE UserAuthority (
	id BIGINT(20) NOT NULL AUTO_INCREMENT,
	authority VARCHAR(255) NULL DEFAULT NULL,
	user_id BIGINT(20) NOT NULL,
	PRIMARY KEY (id),
	INDEX FK_UserAuthority_user_id (user_id),
	CONSTRAINT FK_UserAuthority_user_id FOREIGN KEY (user_id) REFERENCES User (id)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;
