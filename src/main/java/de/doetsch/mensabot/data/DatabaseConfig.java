package de.doetsch.mensabot.data;

import io.github.cdimascio.dotenv.Dotenv;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.client.SSLMode;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import name.nkonev.r2dbc.migrate.core.PostgreSqlQueries;
import name.nkonev.r2dbc.migrate.core.R2dbcMigrate;
import name.nkonev.r2dbc.migrate.core.R2dbcMigrateProperties;
import name.nkonev.r2dbc.migrate.reader.ReflectionsClasspathResourceReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;

public class DatabaseConfig {
	
	private static final Logger logger = LogManager.getLogger(DatabaseConfig.class);
	
	private static final Dotenv dotenv = Dotenv.load();
	private static final ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
			.host(dotenv.get("DATABASE_HOST"))
			.port(Integer.parseInt(dotenv.get("DATABASE_PORT")))
			.database(dotenv.get("DATABASE_NAME"))
			.username(dotenv.get("DATABASE_USERNAME"))
			.password(dotenv.get("DATABASE_PASSWORD"))
			.enableSsl()
			.sslMode(SSLMode.REQUIRE)
			.build()
	);
	public static Mono<Connection> getConnection(){
		return Mono.from(connectionFactory.create());
	}
	
	public static Mono<Boolean> initializeDatabase(){
		R2dbcMigrateProperties properties = new R2dbcMigrateProperties();
		properties.setResourcesPaths(List.of("db/migration"));
		return R2dbcMigrate.migrate(connectionFactory, properties, new ReflectionsClasspathResourceReader(), new PostgreSqlQueries(dotenv.get("DATABASE_NAME"), "migrations", "migrations_lock"))
				.then(Mono.just(true))
				.onErrorResume(err -> {
					logger.error("Error while initializing database", err);
					return Mono.just(false);
				});
	}
	
	public static Mono<Integer> getRowsUpdated(Result result){
		return Mono.from(result.getRowsUpdated()).cast(Object.class).map(o -> {
			// wtf why is it returning a Mono<Long> that actually contains an Integer??
			if(o instanceof Integer) return (int)o;
			else if(o instanceof Long) return (int)(long)o;
			else return -1;
		});
	}
	
}
