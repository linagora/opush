CREATE TABLE synced_collection (
	credentials text,
	device text,
	collection_id int,
	analysed_sync_collection text,
	PRIMARY KEY (credentials, device, collection_id)
);