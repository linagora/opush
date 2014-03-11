CREATE TABLE synced_collection (
	credentials text,
	device text,
	collection_id int,
	analysed_sync_collection text,
	PRIMARY KEY (credentials, device, collection_id)
);

CREATE TABLE monitored_collection (
	credentials text,
	device text,
	analysed_sync_collections set<text>,
	PRIMARY KEY (credentials, device)
);

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

CREATE TABLE windowing (
	id uuid,
	change_index int,
	change_type text,
	change_value text,
	PRIMARY KEY ((id), change_index, change_type)
);

CREATE TABLE snapshot_index (
	device_id text,
	collection_id int,
	sync_key uuid,
	snapshot_id uuid,
	PRIMARY KEY ((device_id), collection_id, sync_key)
);

CREATE TABLE snapshot (
	id uuid,
	snapshot text,
	PRIMARY KEY (id)
);

CREATE TABLE schema_version (
	id int,
	version int,
	date timestamp,
	PRIMARY KEY ((id), version)
) with clustering order by (version desc);