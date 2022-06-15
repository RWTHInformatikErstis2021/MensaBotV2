package de.doetsch.mensabot.main;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class BotMain {
	
	private static final Logger logger = LogManager.getLogger(BotMain.class);
	
	public static void main(String[] args){
		logger.info("Program starting");
		
		DiscordClient discordClient = DiscordClientBuilder.create("TOKEN")
				.build();
		discordClient.withGateway(client -> Mono.when(
				EventHandler.subscribe(client)
		).then(client.onDisconnect())).block();
		
		logger.info("Program exiting");
	}
	
}
