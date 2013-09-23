-- 자산그룹
CREATE TABLE AssetGroup (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '자산그룹 일련번호',
	username VARCHAR(255) NOT NULL COMMENT '소유회원 고유이름',
	name VARCHAR(255) NOT NULL COMMENT '자산그룹 이름',
	assetType BIGINT(20),
	PRIMARY KEY (id)
);

-- 자산
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

-- 기입그룹
CREATE TABLE EntryGroup (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '기입그룹 일련번호',
	username VARCHAR(255) NOT NULL COMMENT '소유회원 고유이름',
	name VARCHAR(255) NOT NULL COMMENT '기입그룹 이름',
	entryType BIGINT(20),
	PRIMARY KEY (id)
);

-- 기입
CREATE TABLE Entry (
	id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '기입 일련번호',
	asset_id BIGINT(20),
	entryGroup_id BIGINT(20),
	amount BIGINT(20) NOT NULL DEFAULT 0 COMMENT '총액',
	createdDate TIMESTAMP NOT NULL DEFAULT NOW() COMMENT '생성일',
	memo VARCHAR(255) NULL DEFAULT NULL COMMENT '기록',
	isDoubleEntry TINYINT NOT NULL DEFAULT 0 COMMENT '이체기록여부',
	PRIMARY KEY (id),
	INDEX FK_Entry_asset_id (asset_id),
	INDEX FK_Entry_entryGroup_id (entryGroup_id),
	CONSTRAINT FK_Entry_asset_id FOREIGN KEY (asset_id) REFERENCES Asset (id),
	CONSTRAINT FK_Entry_entryGroup_id FOREIGN KEY (entryGroup_id) REFERENCES EntryGroup (id)
)