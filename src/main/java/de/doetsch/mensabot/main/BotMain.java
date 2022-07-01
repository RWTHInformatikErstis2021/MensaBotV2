package de.doetsch.mensabot.main;

import de.doetsch.mensabot.canteen.DailyMenuMessage;
import de.doetsch.mensabot.commands.CommandHandler;
import de.doetsch.mensabot.data.DatabaseConfig;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class BotMain {
	
	private static final Logger logger = LogManager.getLogger(BotMain.class);
	
	public static final Dotenv dotenv = Dotenv.load();
	
	public static void main(String[] args){
		logger.info("Program starting");
		DiscordClient discordClient = DiscordClientBuilder.create(dotenv.get("DISCORD_BOT_TOKEN"))
				.build();
		DatabaseConfig.initializeDatabase()
				.flatMap(success -> {
					if(!success){
						logger.warn("Database failed to initialize, not starting bot");
						return Mono.empty();
					}
					return discordClient.withGateway(client -> Mono.when(
							EventHandler.subscribe(client),
							CommandHandler.register(client),
							DailyMenuMessage.startScheduledMessages(client)
					).then(client.onDisconnect()));
				})
				.onErrorResume(err -> {
					logger.error("UNEXPECTED ERROR: there is error handling missing somewhere", err);
					return Mono.empty();
				})
				.block();
		logger.info("Program exiting");
	}
	
}
