CREATE TABLE monitored_collection_v2 (
	user text,
	device text,
	analysed_sync_collections set<text>,
	PRIMARY KEY (user, device)
);