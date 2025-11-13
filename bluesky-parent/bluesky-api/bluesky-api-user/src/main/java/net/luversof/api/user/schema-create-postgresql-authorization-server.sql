-- Spring Authorization Server Schema for Token Exchange
-- 이 테이블들은 Token Exchange Grant 기능을 위해 필요합니다
-- Gate에서 GitHub Token을 JWT로 변환할 때 사용

-- OAuth2 Client 등록 정보 (bluesky-web-gate 등)
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
	id varchar(100) NOT NULL,
	client_id varchar(100) NOT NULL,
	client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	client_secret varchar(200) DEFAULT NULL,
	client_secret_expires_at timestamp DEFAULT NULL,
	client_name varchar(200) NOT NULL,
	client_authentication_methods varchar(1000) NOT NULL,
	authorization_grant_types varchar(1000) NOT NULL,
	redirect_uris varchar(1000) DEFAULT NULL,
	post_logout_redirect_uris varchar(1000) DEFAULT NULL,
	scopes varchar(1000) NOT NULL,
	client_settings varchar(2000) NOT NULL,
	token_settings varchar(2000) NOT NULL,
	PRIMARY KEY (id)
);

-- Authorization Code와 Access Token 저장
CREATE TABLE IF NOT EXISTS oauth2_authorization (
	id varchar(100) NOT NULL,
	registered_client_id varchar(100) NOT NULL,
	principal_name varchar(200) NOT NULL,
	authorization_grant_type varchar(100) NOT NULL,
	authorized_scopes varchar(1000) DEFAULT NULL,
	attributes text DEFAULT NULL,
	state varchar(500) DEFAULT NULL,
	authorization_code_value text DEFAULT NULL,
	authorization_code_issued_at timestamp DEFAULT NULL,
	authorization_code_expires_at timestamp DEFAULT NULL,
	authorization_code_metadata text DEFAULT NULL,
	access_token_value text DEFAULT NULL,
	access_token_issued_at timestamp DEFAULT NULL,
	access_token_expires_at timestamp DEFAULT NULL,
	access_token_metadata text DEFAULT NULL,
	access_token_type varchar(100) DEFAULT NULL,
	access_token_scopes varchar(1000) DEFAULT NULL,
	oidc_id_token_value text DEFAULT NULL,
	oidc_id_token_issued_at timestamp DEFAULT NULL,
	oidc_id_token_expires_at timestamp DEFAULT NULL,
	oidc_id_token_metadata text DEFAULT NULL,
	refresh_token_value text DEFAULT NULL,
	refresh_token_issued_at timestamp DEFAULT NULL,
	refresh_token_expires_at timestamp DEFAULT NULL,
	refresh_token_metadata text DEFAULT NULL,
	user_code_value text DEFAULT NULL,
	user_code_issued_at timestamp DEFAULT NULL,
	user_code_expires_at timestamp DEFAULT NULL,
	user_code_metadata text DEFAULT NULL,
	device_code_value text DEFAULT NULL,
	device_code_issued_at timestamp DEFAULT NULL,
	device_code_expires_at timestamp DEFAULT NULL,
	device_code_metadata text DEFAULT NULL,
	PRIMARY KEY (id)
);

-- 사용자 동의 정보
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
	registered_client_id varchar(100) NOT NULL,
	principal_name varchar(200) NOT NULL,
	authorities varchar(1000) NOT NULL,
	PRIMARY KEY (registered_client_id, principal_name)
);
