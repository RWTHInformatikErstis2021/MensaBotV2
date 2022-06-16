package de.doetsch.mensabot.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public abstract class Command {
	
	abstract ApplicationCommandRequest getCommand();
	abstract Mono<Void> execute(ApplicationCommandInteractionEvent event);
	
}
