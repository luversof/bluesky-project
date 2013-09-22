-- 자산그룹
CREATE TABLE AssetGroup (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '자산그룹 일련번호',
	username VARCHAR(255) NOT NULL COMMENT '소유회원 고유이름',
	name VARCHAR(255) NOT NULL COMMENT '자산그룹 이름',
	assetType BIGINT(20),
	PRIMARY KEY (id)
);

CREATE TABLE Asset (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '자산 일련번호',
	username VARCHAR(255) NOT NULL COMMENT '소유회원 고유이름',
	name VARCHAR(255) NOT NULL COMMENT '자산이름',
	amount BIGINT(20) NOT NULL DEFAULT 0 COMMENT '총액',
	enable TINYINT NOT NULL DEFAULT 0 COMMENT '사용여부',
	assetGroup_id BIGINT(20) NOT NULL,
	PRIMARY KEY (id),
	INDEX FK_Asset_username (username),
	INDEX FK_Asset_assetGroup_id (assetGroup_id),
	CONSTRAINT FK_Asset_assetGroup_id FOREIGN KEY (assetGroup_id) REFERENCES AssetGroup (id)
);

CREATE TABLE EntryGroup (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '기입그룹 일련번호',
	username VARCHAR(255) NULL DEFAULT NULL COMMENT '소유회원 고유이름',
	name VARCHAR(255) NULL DEFAULT NULL COMMENT '기입그룹 이름',
	entryType BIGINT(20),
	PRIMARY KEY (id)
);

#아직 만들다 말았음
CREATE TABLE Entry (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '기입 일련번호',
	asset_id BIGINT(20),
	entryType_id BIGINT(20),
	entryGroup_id BIGINT(20),
	amount BIGINT(20) NOT NULL DEFAULT 0 COMMENT '총액'
)