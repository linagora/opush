CREATE TABLE windowing_index (
	user text,
	device_id text,
	collection_id int,
	sync_key uuid,
	windowing_id uuid,
	windowing_index int,
	PRIMARY KEY (user, device_id, collection_id, sync_key)
);

CREATE TABLE windowing (
	id uuid,
	change_index int,
	change_type text,
	change_value text,
	PRIMARY KEY ((id), change_index, change_type)
);