package com.ssafy.e102.graphhopper;

import static com.ssafy.e102.graphhopper.ieum.IeumEnum.YesNoUnknown.NO;
import static com.ssafy.e102.graphhopper.ieum.IeumEnum.YesNoUnknown.YES;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ssafy.e102.graphhopper.ieum.IeumEnum.YesNoUnknown;

public class RoutingSegmentOverrideStore {

	private static final Logger log = LoggerFactory.getLogger(RoutingSegmentOverrideStore.class);
	private static final String SELECT_SQL = "select edge_id, walk_access from routing_segment_overrides";
	private static final String UNDEFINED_TABLE_SQL_STATE = "42P01";

	private final AtomicReference<Map<Long, YesNoUnknown>> overridesRef = new AtomicReference<>(Map.of());

	public void reload() {
		Map<Long, YesNoUnknown> overrides = loadOverrides();
		overridesRef.set(overrides);
		log.info("loaded routing segment overrides count={}", overrides.size());
	}

	public YesNoUnknown getWalkAccessOverride(long edgeId) {
		return overridesRef.get().get(edgeId);
	}

	public int size() {
		return overridesRef.get().size();
	}

	private Map<Long, YesNoUnknown> loadOverrides() {
		try (Connection connection = openConnection();
			 PreparedStatement statement = connection.prepareStatement(SELECT_SQL);
			 ResultSet resultSet = statement.executeQuery()) {
			Map<Long, YesNoUnknown> overrides = new LinkedHashMap<>();
			while (resultSet.next()) {
				long edgeId = resultSet.getLong("edge_id");
				YesNoUnknown walkAccess = parseWalkAccess(resultSet.getString("walk_access"));
				if (walkAccess != null) {
					overrides.put(edgeId, walkAccess);
				}
			}
			return Map.copyOf(overrides);
		} catch (SQLException exception) {
			if (UNDEFINED_TABLE_SQL_STATE.equals(exception.getSQLState())) {
				log.warn("routing_segment_overrides table does not exist yet; starting with empty overlay");
				return Map.of();
			}
			throw new IllegalStateException("failed to load routing segment overrides", exception);
		}
	}

	private Connection openConnection() throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException exception) {
			throw new IllegalStateException("postgresql jdbc driver is not available", exception);
		}
		String jdbcUrl = firstNonBlank(
			System.getenv("DB_URL"),
			System.getenv("SPRING_DATASOURCE_URL"),
			buildFallbackJdbcUrl());
		Properties properties = new Properties();
		putIfPresent(properties, "user", firstNonBlank(System.getenv("DB_USERNAME"), System.getenv("POSTGRES_USER")));
		putIfPresent(properties, "password", firstNonBlank(System.getenv("DB_PASSWORD"), System.getenv("POSTGRES_PASSWORD")));
		putIfPresent(properties, "sslmode", firstNonBlank(System.getenv("DB_SSLMODE"), System.getenv("PGSSLMODE")));
		return DriverManager.getConnection(jdbcUrl, properties);
	}

	private String buildFallbackJdbcUrl() {
		String host = firstNonBlank(System.getenv("PGHOST"), "postgres");
		String port = firstNonBlank(System.getenv("PGPORT"), "5432");
		String database = firstNonBlank(System.getenv("PGDATABASE"), System.getenv("POSTGRES_DB"), "e102");
		return "jdbc:postgresql://" + host + ":" + port + "/" + database;
	}

	private YesNoUnknown parseWalkAccess(String value) {
		if (value == null) {
			return null;
		}
		if (NO.name().equalsIgnoreCase(value)) {
			return NO;
		}
		if (YES.name().equalsIgnoreCase(value)) {
			return YES;
		}
		log.warn("ignoring unsupported routing override walk_access={}", value);
		return null;
	}

	private static String firstNonBlank(String... candidates) {
		for (String candidate : candidates) {
			if (candidate != null && !candidate.isBlank()) {
				return candidate;
			}
		}
		return null;
	}

	private static void putIfPresent(Properties properties, String key, String value) {
		if (value != null && !value.isBlank()) {
			properties.setProperty(key, value);
		}
	}
}
