-- 1. 사용자 계정 테이블
CREATE TABLE users (
	username VARCHAR(50) PRIMARY KEY,
	password VARCHAR(100) NOT NULL,
	enabled BOOLEAN NOT NULL
);

-- 2. 권한(역할) 테이블
CREATE TABLE authorities (
	username VARCHAR(50) NOT NULL,
	authority VARCHAR(50) NOT NULL,
	CONSTRAINT fk_authorities_users FOREIGN KEY(username) REFERENCES users(username)
);

-- 권한 중복 방지
CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);

-- oauth2-client-schema-postgres.sql 파일 내용 추가
CREATE TABLE oauth2_authorized_client (
  client_registration_id varchar(100) NOT NULL,
  principal_name varchar(200) NOT NULL,
  access_token_type varchar(100) NOT NULL,
  access_token_value bytea NOT NULL,
  access_token_issued_at timestamp NOT NULL,
  access_token_expires_at timestamp NOT NULL,
  access_token_scopes varchar(1000) DEFAULT NULL,
  refresh_token_value bytea DEFAULT NULL,
  refresh_token_issued_at timestamp DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);

-- 개별 사용 테이블 
CREATE TABLE "UserInfo" (
	"id" UUID NOT NULL PRIMARY KEY,
	"username" VARCHAR(50) NOT NULL,
	"password" VARCHAR(200),
	"provider" VARCHAR(20),
	"provider_id" VARCHAR(100),
	"email" VARCHAR(100),
	"avatar_url" VARCHAR(500)
);

CREATE UNIQUE INDEX uk_userInfo_username ON "UserInfo" ("username");
CREATE UNIQUE INDEX uk_userInfo_provider_providerId ON "UserInfo" ("provider", "provider_id");

-- OAuth2 로그인 사용자 역할(2026-11-17 추가). 위 authorities 는 폼 로그인용 users 를 FK 로 물어 GitHub 사용자(UserInfo)에게 못 쓴다.
-- 로그인할 때 user API 가 읽어 client-user 가 ROLE_USER 에 더한다. 세션에 들어가므로 바꾸면 다시 로그인해야 반영된다.
-- 역할 계층(앱에서): ROLE_MASTER > ROLE_ADMIN > ROLE_USER
CREATE TABLE "UserAuthority" (
	"userInfo_id" UUID NOT NULL,
	"authority" VARCHAR(50) NOT NULL,
	PRIMARY KEY ("userInfo_id", "authority"),
	CONSTRAINT fk_userAuthority_userInfo FOREIGN KEY ("userInfo_id") REFERENCES "UserInfo" ("id") ON DELETE CASCADE
);