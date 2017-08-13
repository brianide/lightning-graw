CREATE TABLE IF NOT EXISTS configs (
	id BIGINT IDENTITY,
	guild_id VARCHAR(32) UNIQUE NOT NULL,
	repo_url VARCHAR(256) NOT NULL,
	username VARCHAR(256) NOT NULL,
	password VARBINARY(256) NOT NULL,
	interval INT,
	responsive BIT,
	last_rev BIGINT,
	date_fmt VARCHAR(256),
	message_fmt VARCHAR(256),
	channel_id VARCHAR(32),
	maintainers_id VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS meta_info (
	id BIGINT IDENTITY,
	meta_key VARCHAR(128) UNIQUE NOT NULL,
	meta_value VARCHAR(512)
);
