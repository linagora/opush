
--
-- Create opush_folder_sync_state
--
 
BEGIN;
 
CREATE TABLE opush_folder_sync_state (
	id		SERIAL PRIMARY KEY,
	sync_key	VARCHAR(64) UNIQUE NOT NULL,
	device_id	INTEGER NOT NULL REFERENCES opush_device(id) ON DELETE CASCADE,
	collection_id	INTEGER NOT NULL
);

--
-- Insert datas in opush_folder_sync_state
--

CREATE TEMP TABLE main_folder_mapping (
	collection_id		INTEGER,
	collection_min_length	INTEGER,
	device_id		INTEGER
);

INSERT INTO main_folder_mapping (collection_min_length, device_id)
	SELECT MIN(LENGTH(collection)), device_id FROM opush_folder_mapping
	GROUP BY device_id;

UPDATE main_folder_mapping SET collection_id = id FROM opush_folder_mapping
	WHERE LENGTH(collection) = collection_min_length
	AND main_folder_mapping.device_id = opush_folder_mapping.device_id;


INSERT INTO opush_folder_sync_state (collection_id, device_id, sync_key)
	SELECT main_folder_mapping.collection_id, main_folder_mapping.device_id, sync_key 
		FROM main_folder_mapping, opush_sync_state
	WHERE main_folder_mapping.collection_id = opush_sync_state.collection_id
	AND main_folder_mapping.device_id = opush_sync_state.device_id;

INSERT INTO opush_folder_sync_state (collection_id, device_id, sync_key)
	SELECT collection_id, device_id, sync_key FROM opush_sync_state
	WHERE NOT EXISTS (SELECT collection_id FROM opush_folder_sync_state
		WHERE opush_folder_sync_state.device_id = opush_sync_state.device_id);

--
-- Update opush_folder_mapping
--

ALTER TABLE opush_folder_mapping ADD COLUMN folder_sync_state_id INTEGER 
	REFERENCES opush_folder_sync_state(id) ON DELETE CASCADE; 

UPDATE opush_folder_mapping SET folder_sync_state_id = opush_folder_sync_state.id FROM opush_folder_sync_state
	WHERE opush_folder_mapping.device_id = opush_folder_sync_state.device_id
	AND opush_folder_sync_state.id = (SELECT MAX(id) FROM opush_folder_sync_state
		WHERE opush_folder_sync_state.device_id = opush_folder_mapping.device_id);

ALTER TABLE opush_folder_mapping ALTER folder_sync_state_id SET NOT NULL;
ALTER TABLE opush_folder_mapping DROP device_id;
ALTER TABLE opush_folder_sync_state DROP collection_id;

COMMIT;
