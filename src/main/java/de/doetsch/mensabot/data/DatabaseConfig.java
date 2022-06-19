package de.doetsch.mensabot.data;

import io.github.cdimascio.dotenv.Dotenv;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

public class DatabaseConfig {
	
	private static final Dotenv dotenv = Dotenv.load();
	private static final ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
			.host(dotenv.get("DATABASE_HOST"))
			.database(dotenv.get("DATABASE_NAME"))
			.username(dotenv.get("DATABASE_USERNAME"))
			.password(dotenv.get("DATABASE_PASSWORD"))
			.build()
	);
	public static Mono<Connection> getConnection(){
		return Mono.from(connectionFactory.create());
	}
	
	public static Mono<Void> initializeDatabase(){
		return getConnection().flatMapMany(connection -> connection.createStatement(
				"CREATE TABLE IF NOT EXISTS ratings (\n" +
						"    userId BIGINT,\n" +
						"    meal VARCHAR(200),\n" +
						"    rating INTEGER,\n" +
						"    PRIMARY KEY (userId, meal)\n" +
						");"
		).execute()).then();
	}
	
}
