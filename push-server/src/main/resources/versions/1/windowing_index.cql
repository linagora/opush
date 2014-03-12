CREATE TABLE windowing_index (
	user text,
	device_id text,
	collection_id int,
	sync_key uuid,
	windowing_id uuid,
	windowing_kind text,
	windowing_index int,
	PRIMARY KEY (user, device_id, collection_id, sync_key)
);