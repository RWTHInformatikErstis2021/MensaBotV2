package de.doetsch.mensabot.data.types;

import de.doetsch.mensabot.data.DatabaseConfig;
import de.doetsch.mensabot.util.Util;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

public class RatingRepository {
	
	private static final Logger logger = LogManager.getLogger(RatingRepository.class);
	
	private static final BiFunction<Row, RowMetadata, Rating> MAPPING_FUNCTION = (row, rowMetadata) -> new Rating(row.get("userId", Long.class), row.get("meal", String.class), row.get("rating", Integer.class));
	
	public static Mono<List<Rating>> findByMeal(String meal){
		String mealTrimmed = Util.trim(meal, 100);
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("SELECT * FROM ratings WHERE meal=$1")
						.bind("$1", mealTrimmed)
						.execute()
				)
				.flatMap(result -> result.map(MAPPING_FUNCTION))
				.onErrorResume(err -> {
					logger.error("Error while getting ratings for meal " + meal + " from database");
					return Mono.empty();
				})
				.collectList();
	}
	
	public static Mono<Integer> save(Rating rating){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("INSERT INTO ratings (userId, meal, rating) VALUES ($1, $2, $3)" +
								" ON CONFLICT (userId, meal) DO UPDATE SET rating=$3")
						.bind("$1", rating.getUserId())
						.bind("$2", rating.getMeal())
						.bind("$3", rating.getRating())
						.execute()
				)
				.flatMap(Result::getRowsUpdated).cast(Object.class).flatMap(o -> {
					// wtf why is it returning a Mono<Long> that actually contains an Integer??
					if(o instanceof Integer) return Mono.just((int)o);
					else if(o instanceof Long) return Mono.just((int)(long)o);
					else return Mono.just(-1);
				}).next()
				.onErrorResume(err -> {
					logger.error("Error while saving rating", err);
					return Mono.empty();
				});
	}
	
}
