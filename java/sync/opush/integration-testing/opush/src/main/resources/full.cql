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