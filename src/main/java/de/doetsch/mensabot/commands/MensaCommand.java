package de.doetsch.mensabot.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class MensaCommand extends Command {
	
	private final ApplicationCommandRequest command = ApplicationCommandRequest.builder()
			.name("mensa")
			.description("Gibt den Speiseplan der gewählten Mensa aus")
			.build();
	
	@Override
	ApplicationCommandRequest getCommand(){
		return command;
	}
	
	@Override
	public Mono<Void> execute(ApplicationCommandInteractionEvent event){
		return Mono.empty();
	}
	
}
