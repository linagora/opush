CREATE TABLE folder_snapshot (
	user text,
	device_id text,
	sync_key uuid,
	next_collection_id int,
	folders set<text>,
	PRIMARY KEY (user, device_id, sync_key)
) WITH default_time_to_live = 2592000;

CREATE TABLE folder_mapping (
	user text,
	device_id text,
	collection_id int,
	backend_id text,
	data_type text,
	folder text,
	PRIMARY KEY ((user, device_id), collection_id)
) WITH default_time_to_live = 2592000;

CREATE INDEX ON folder_mapping (backend_id);
CREATE INDEX ON folder_mapping (data_type);