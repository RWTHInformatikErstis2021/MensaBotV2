package de.doetsch.mensabot.data.types;

import de.doetsch.mensabot.data.DatabaseConfig;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

public class GuildSettingsRepository {
	
	private static final Logger logger = LogManager.getLogger(GuildSettingsRepository.class);
	
	private static final BiFunction<Row, RowMetadata, GuildSettings> MAPPING_FUNCTION = (row, rowMetadata) -> new GuildSettings(row.get("guildId", Long.class), row.get("defaultCanteenId", Integer.class), row.get("announcementChannelId", Long.class));
	
	public static Mono<GuildSettings> findById(long guildId){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("")
						.execute()
				)
				.flatMap(result -> result.map(MAPPING_FUNCTION))
				.next()
				.onErrorResume(err -> {
					logger.error("Error while getting guild settings for guild " + guildId + " from database", err);
					return Mono.empty();
				});
	}
	
	
	public static Mono<Integer> initialize(long guildId){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("INSERT INTO guilds (guildId, defaultCanteenId, announcementChannelId) VALUES ($1, $2, $3)" +
								"ON CONFLICT (guildId) DO NOTHING")
						.bind("$1", guildId)
						.bind("$2", 1)
						.bindNull("$3", Long.class)
						.execute()
				)
				.flatMap(DatabaseConfig::getRowsUpdated)
				.next()
				.onErrorResume(err -> {
					logger.error("Error while initializing guild settings for guild " + guildId, err);
					return Mono.empty();
				});
	}
	
	public static Mono<Integer> setDefaultCanteenId(long guildId, int canteenId){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("UPDATE guilds SET defaultCanteenId=$2 WHERE guildId=$1")
						.bind("$1", guildId)
						.bind("$2", canteenId)
						.execute()
				)
				.flatMap(DatabaseConfig::getRowsUpdated)
				.next()
				.onErrorResume(err -> {
					logger.error("Error while saving default canteen id " + canteenId + " for guild " + guildId, err);
					return Mono.empty();
				});
	}
	
	public static Mono<Integer> setAnnouncementChannelId(long guildId, long channelId){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("UPDATE guilds SET announcementChannelId=$2 WHERE guildId=$1")
						.bind("$1", guildId)
						.bind("$2", channelId)
						.execute()
				)
				.flatMap(DatabaseConfig::getRowsUpdated)
				.next()
				.onErrorResume(err -> {
					logger.error("Error while saving announcement channel id " + channelId + " for guild " + guildId, err);
					return Mono.empty();
				});
	}
	
}
