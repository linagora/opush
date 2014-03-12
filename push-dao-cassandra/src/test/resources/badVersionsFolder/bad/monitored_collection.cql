CREATE TABLE monitored_collection (
	credentials text,
	device text,
	analysed_sync_collections set<text>,
	PRIMARY KEY (credentials, device)
);