CREATE TABLE contact_creation (
	user text,
	device_id text,
	collection_id int,
	hash text,
	server_id text,
	PRIMARY KEY ((user, device_id, collection_id), hash)
) WITH default_time_to_live = 864000 ;

CREATE INDEX ON contact_creation (server_id);