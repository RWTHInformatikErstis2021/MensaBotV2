package de.doetsch.mensabot.data;

import io.github.cdimascio.dotenv.Dotenv;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class DatabaseConfig {
	
	private static final Logger logger = LogManager.getLogger(DatabaseConfig.class);
	
	private static final Dotenv dotenv = Dotenv.load();
	private static final ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
			.host(dotenv.get("DATABASE_HOST"))
			.port(Integer.parseInt(dotenv.get("DATABASE_PORT")))
			.database(dotenv.get("DATABASE_NAME"))
			.username(dotenv.get("DATABASE_USERNAME"))
			.password(dotenv.get("DATABASE_PASSWORD"))
			.build()
	);
	public static Mono<Connection> getConnection(){
		return Mono.from(connectionFactory.create());
	}
	
	public static Mono<Boolean> initializeDatabase(){
		if(true) return Mono.just(true); // TODO remove
		// (i currently dont have postgresql set up on my machine and am lazy to set it up rn)
		return getConnection().flatMapMany(connection -> connection.createStatement(
				"CREATE TABLE IF NOT EXISTS ratings (\n" +
						"    userId BIGINT,\n" +
						"    meal VARCHAR(100),\n" +
						"    rating INTEGER,\n" +
						"    PRIMARY KEY (userId, meal)\n" +
						");"
		).execute()).then(Mono.just(true))
				.onErrorResume(err -> {
					logger.error("Error while initializing database", err);
					return Mono.just(false);
				});
	}
	
}
