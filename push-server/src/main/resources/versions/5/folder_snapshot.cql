CREATE TABLE folder_snapshot (
	user text,
	device_id text,
	sync_key uuid,
	next_collection_id int,
	folder_type text,
	folders set<text>,
	PRIMARY KEY ((user, device_id, sync_key), folder_type)
) WITH default_time_to_live = 2592000;