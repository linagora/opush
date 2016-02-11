CREATE TABLE windowing_to_snapshot (
	device_id text,
	collection_id int,
	sync_key uuid,
	snapshot_id uuid,
	PRIMARY KEY ((device_id), collection_id, sync_key)
) WITH default_time_to_live = 2592000;
