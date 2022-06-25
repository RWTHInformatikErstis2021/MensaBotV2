package de.doetsch.mensabot.data.types;

import de.doetsch.mensabot.data.DatabaseConfig;
import de.doetsch.mensabot.util.Util;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

public class RatingRepository {
	
	private static final Logger logger = LogManager.getLogger(RatingRepository.class);
	
	private static final BiFunction<Row, RowMetadata, Rating> MAPPING_FUNCTION = (row, rowMetadata) -> new Rating(row.get("userId", Long.class), row.get("city", String.class), row.get("meal", String.class), row.get("rating", Integer.class));
	
	public static Mono<List<Rating>> findByMeal(String city, String meal){
		String cityTrimmed = Util.trim(city, 50);
		String mealTrimmed = Util.trim(meal, 100);
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("SELECT * FROM ratings WHERE city=$1 AND meal=$2")
						.bind("$1", cityTrimmed)
						.bind("$2", mealTrimmed)
						.execute()
				)
				.flatMap(result -> result.map(MAPPING_FUNCTION))
				.onErrorResume(err -> {
					logger.error("Error while getting ratings for meal " + meal + " (" + city + ") from database", err);
					return Mono.empty();
				})
				.collectList();
	}
	
	public static Mono<Integer> save(Rating rating){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("INSERT INTO ratings (userId, city, meal, rating) VALUES ($1, $2, $3, $4)" +
								" ON CONFLICT (userId, city, meal) DO UPDATE SET rating=$4")
						.bind("$1", rating.getUserId())
						.bind("$2", Util.trim(rating.getCity(), 50))
						.bind("$3", Util.trim(rating.getMeal(), 100))
						.bind("$4", rating.getRating())
						.execute()
				)
				.flatMap(DatabaseConfig::getRowsUpdated)
				.next()
				.onErrorResume(err -> {
					logger.error("Error while saving rating", err);
					return Mono.empty();
				});
	}
	
}
