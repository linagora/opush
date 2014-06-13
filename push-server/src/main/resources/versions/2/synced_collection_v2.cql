CREATE TABLE synced_collection_v2 (
	user text,
	device text,
	collection_id int,
	analysed_sync_collection text,
	PRIMARY KEY (user, device, collection_id)
);