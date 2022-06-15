package de.doetsch.mensabot.main;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class EventHandler {
	
	private static final Logger logger = LogManager.getLogger(EventHandler.class);
	
	/**
	 * Subscribes to all important events used by the bot
	 * @param client The client on which the events should be subscribed on
	 * @return A {@link Mono} with the subscriptions applied
	 */
	public static Mono<Void> subscribe(GatewayDiscordClient client){
		return Mono.when(
				/* successfully logged in */
				client.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> logger.info("Logged in as {} ({})", event.getSelf().getTag(), event.getSelf().getId().asString())))
		);
	}
	
}
