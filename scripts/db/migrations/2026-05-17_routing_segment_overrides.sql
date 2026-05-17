CREATE TABLE IF NOT EXISTS routing_segment_overrides (
    edge_id BIGINT PRIMARY KEY,
    walk_access VARCHAR(30) NOT NULL,
    CONSTRAINT fk_routing_segment_overrides_edge_id
        FOREIGN KEY (edge_id)
        REFERENCES road_segments (edge_id)
        ON DELETE CASCADE
);
