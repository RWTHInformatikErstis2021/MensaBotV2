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
		if(true) return Mono.just(List.of()); // TODO just set up a damn database and remove this shit
		// discord only allows select menu option values to be 100 characters long
		// meaning they are trimmed to 100 characters if longer
		String mealTrimmed = Util.trim(meal, 100);
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("SELECT * FROM ratings WHERE meal=:meal")
						.bind("meal", mealTrimmed)
						.execute()
				)
				.flatMap(result -> result.map(MAPPING_FUNCTION))
				.onErrorResume(err -> {
					logger.error("Error while getting ratings for meal " + meal + " from database");
					return Mono.empty();
				})
				.collectList();
	}
	
	public static Mono<Long> save(Rating rating){
		if(true) return Mono.just(1L); // TODO like fr it isnt that hard
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("INSERT INTO ratings (userId, meal, rating) VALUES (:userId, :meal, :rating)" +
								" ON CONFLICT DO UPDATE SET rating=:rating")
						.bind("userId", rating.getUserId())
						.bind("meal", rating.getMeal())
						.bind("rating", rating.getRating())
						.execute()
				)
				.flatMap(Result::getRowsUpdated).next()
				.onErrorResume(err -> {
					logger.error("Error while saving rating", err);
					return Mono.empty();
				});
	}
	
}
