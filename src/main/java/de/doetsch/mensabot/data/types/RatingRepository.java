package de.doetsch.mensabot.data.types;

import de.doetsch.mensabot.data.DatabaseConfig;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

public class RatingRepository {
	
	private static final BiFunction<Row, RowMetadata, Rating> MAPPING_FUNCTION = (row, rowMetadata) -> new Rating(row.get("userId", Long.class), row.get("meal", String.class), row.get("rating", Integer.class));
	
	public Flux<Rating> findByMeal(String meal){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("SELECT * FROM ratings WHERE meal=:meal")
						.bind("meal", meal)
						.execute()
				)
				.flatMap(result -> result.map(MAPPING_FUNCTION));
	}
	
	public Mono<Long> save(Rating rating){
		return DatabaseConfig.getConnection()
				.flatMapMany(connection -> connection.createStatement("INSERT INTO ratings (userId, meal, rating) VALUES (:userId, :meal, :rating)" +
								" ON CONFLICT DO UPDATE SET rating=:rating")
						.bind("userId", rating.getUserId())
						.bind("meal", rating.getMeal())
						.bind("rating", rating.getRating())
						.execute()
				)
				.flatMap(Result::getRowsUpdated).next();
	}
	
}