CREATE TABLE IF NOT EXISTS routing_segment_overrides (
    edge_id BIGINT PRIMARY KEY,
    walk_access VARCHAR(30),
    stairs_state VARCHAR(30),
    width_state VARCHAR(30),
    braille_block_state VARCHAR(30),
    CONSTRAINT fk_routing_segment_overrides_edge_id
        FOREIGN KEY (edge_id)
        REFERENCES road_segments (edge_id)
        ON DELETE CASCADE
);

ALTER TABLE routing_segment_overrides
    ALTER COLUMN walk_access DROP NOT NULL;

ALTER TABLE routing_segment_overrides
    ADD COLUMN IF NOT EXISTS stairs_state VARCHAR(30);

ALTER TABLE routing_segment_overrides
    ADD COLUMN IF NOT EXISTS width_state VARCHAR(30);

ALTER TABLE routing_segment_overrides
    ADD COLUMN IF NOT EXISTS braille_block_state VARCHAR(30);
