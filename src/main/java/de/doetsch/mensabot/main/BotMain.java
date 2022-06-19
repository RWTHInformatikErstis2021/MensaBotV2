package de.doetsch.mensabot.main;

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
		DatabaseConfig.initializeDatabase().then(discordClient.withGateway(client -> Mono.when(
				EventHandler.subscribe(client),
				CommandHandler.register(client)
		).then(client.onDisconnect()))).block();
		
		logger.info("Program exiting");
	}
	
}
