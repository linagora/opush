CREATE TABLE snapshot_index (
	device_id text,
	collection_id int,
	sync_key uuid,
	snapshot_id uuid,
	PRIMARY KEY ((device_id), collection_id, sync_key)
);