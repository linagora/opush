CREATE TABLE windowing (
	id uuid,
	change_index int,
	change_type text,
	change_value text,
	PRIMARY KEY ((id), change_index, change_type)
);