-- Master Database Setup Script for Roads Navigation System

-- 1. Enable PostGIS spatial extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Table: saved_simulations
CREATE TABLE IF NOT EXISTS saved_simulations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    start_point geometry(Point, 4326) NOT NULL,
    end_point geometry(Point, 4326) NOT NULL,
    direction VARCHAR(32) NOT NULL DEFAULT 'BOTH'
);

ALTER TABLE saved_simulations ADD COLUMN IF NOT EXISTS direction VARCHAR(32) NOT NULL DEFAULT 'BOTH';

-- 3. Table: simulation_vehicles
CREATE TABLE IF NOT EXISTS simulation_vehicles (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES saved_simulations(id) ON DELETE CASCADE,
    average_speed DOUBLE PRECISION NOT NULL,
    loop BOOLEAN NOT NULL,
    speed_noise DOUBLE PRECISION NOT NULL,
    start_delay_seconds DOUBLE PRECISION NOT NULL,
    time_noise_seconds DOUBLE PRECISION NOT NULL DEFAULT 0,
    x_coordinate_noise DOUBLE PRECISION NOT NULL DEFAULT 0,
    y_coordinate_noise DOUBLE PRECISION NOT NULL DEFAULT 0
);

-- 4. Table: simulation_points
CREATE TABLE IF NOT EXISTS simulation_points (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES saved_simulations(id) ON DELETE CASCADE,
    point_type VARCHAR(32) NOT NULL,
    run_index INTEGER NOT NULL,
    step_index INTEGER NOT NULL,
    time_seconds DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    road_id INTEGER DEFAULT 0 NOT NULL,
    direction VARCHAR(32) DEFAULT 'FORWARD',
    coordinate geometry(Point, 4326) NOT NULL
);

ALTER TABLE simulation_points ADD COLUMN IF NOT EXISTS direction VARCHAR(32) DEFAULT 'FORWARD';

-- 5. Spatial & Foreign Key Indexes
CREATE INDEX IF NOT EXISTS idx_saved_simulations_start_point
    ON saved_simulations USING GIST (start_point);

CREATE INDEX IF NOT EXISTS idx_saved_simulations_end_point
    ON saved_simulations USING GIST (end_point);

CREATE INDEX IF NOT EXISTS idx_simulation_vehicles_simulation_id
    ON simulation_vehicles (simulation_id);

CREATE INDEX IF NOT EXISTS idx_simulation_points_simulation_id
    ON simulation_points (simulation_id);

CREATE INDEX IF NOT EXISTS idx_simulation_points_road_id
    ON simulation_points (road_id);

CREATE INDEX IF NOT EXISTS idx_simulation_points_coordinate
    ON simulation_points USING GIST (coordinate);
