package de.doetsch.mensabot.commands;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public abstract class Command {
	
	abstract ApplicationCommandRequest getCommand();
	abstract Mono<Void> execute(ChatInputInteractionEvent event);
	public Mono<Void> autoComplete(ChatInputAutoCompleteEvent event){
		return Mono.empty();
	}
	
}
