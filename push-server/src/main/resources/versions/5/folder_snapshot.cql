CREATE TABLE folder_snapshot (
	user text,
	device_id text,
	sync_key uuid,
	next_collection_id int,
	folder_type text,
	folders set<text>,
	PRIMARY KEY ((user, device_id, sync_key), folder_type)
) WITH default_time_to_live = 2592000;

CREATE TABLE folder_mapping (
	user text,
	device_id text,
	collection_id int,
	folder text,
	PRIMARY KEY ((user, device_id), collection_id)
) WITH default_time_to_live = 2592000;

CREATE TABLE folder_reverse_mapping (
	user text,
	device_id text,
	backend_id text,
	folder_type text,
	folder text,
	PRIMARY KEY ((user, device_id), backend_id, folder_type)
) WITH default_time_to_live = 2592000;