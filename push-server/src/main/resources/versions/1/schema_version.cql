CREATE TABLE schema_version (
	id int,
	version int,
	date timestamp,
	PRIMARY KEY ((id), version)
) with clustering order by (version desc);