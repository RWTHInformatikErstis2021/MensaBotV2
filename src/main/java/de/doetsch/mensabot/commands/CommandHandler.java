package de.doetsch.mensabot.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {
	
	private static final Logger logger = LogManager.getLogger(CommandHandler.class);
	
	public Mono<Void> register(GatewayDiscordClient client){
		return Mono.zip(
				Mono.fromCallable(() -> {
					Map<String, Command> commands = new HashMap<>();
					for(Command command : new Command[]{
							new MensaCommand()
					}){
						commands.put(command.getCommand().name(), command);
					}
					return commands;
				}),
				client.getApplicationInfo()
		).flatMap(TupleUtils.function((commands, applicationInfo) -> Mono.when(
				/* register commands */
				Flux.fromIterable(commands.values())
						.doOnNext(command -> commands.put(command.getCommand().name(), command))
						.flatMap(command -> client.getRestClient().getApplicationService().createGuildApplicationCommand(applicationInfo.getId().asLong(), 0L, command.getCommand())),
				/* register command event */
				client.on(ApplicationCommandInteractionEvent.class, event -> {
					Command command = commands.get(event.getCommandName());
					if(command == null) return event.reply("Der Befehl konnte nicht gefunden werden.");
					else return command.execute(event);
				})
		)));
	}
	
}
