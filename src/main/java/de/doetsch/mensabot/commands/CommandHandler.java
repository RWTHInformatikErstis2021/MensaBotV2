package de.doetsch.mensabot.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {
	
	private static final Logger logger = LogManager.getLogger(CommandHandler.class);
	
	public static Mono<Void> register(GatewayDiscordClient client){
		return Mono.zip(
				Mono.fromCallable(() -> {
					Map<String, Command> commands = new HashMap<>();
					for(Command command : new Command[]{
							new MensaCommand(),
							new RateCommand()
					}){
						commands.put(command.getCommand().name(), command);
					}
					return commands;
				}),
				client.getApplicationInfo().onErrorResume(err -> {
					logger.error("Error while getting application information", err);
					return Mono.empty();
				})
		).flatMap(TupleUtils.function((commands, applicationInfo) -> Mono.when(
				/* register commands */
				Flux.fromIterable(commands.values())
						.doOnNext(command -> commands.put(command.getCommand().name(), command))
						.flatMap(command -> client.getRestClient().getApplicationService()
								.createGuildApplicationCommand(applicationInfo.getId().asLong(), 518442628400939009L, command.getCommand())
								.onErrorResume(err -> {
									logger.error("Error while registering command " + command.getCommand().name(), err);
									return Mono.empty();
								})
						),
				/* register chat command event */
				client.on(ChatInputInteractionEvent.class, event -> {
					Command command = commands.get(event.getCommandName());
					if(command == null) return event.reply("Der Befehl konnte nicht gefunden werden.")
							.onErrorResume(err -> {
								logger.error("Error while sending command not found message", err);
								return Mono.empty();
							});
					else return command.execute(event)
							.onErrorResume(err -> {
								logger.error("Error while executing command " + event.getCommandName(), err);
								return event.reply("Beim Ausführen des Befehls ist ein Fehler aufgetreten.")
										.onErrorResume(err2 -> {
											logger.error("Error while sending command execution error message", err2);
											return Mono.empty();
										});
							});
				}),
				/* register auto completion */
				client.on(ChatInputAutoCompleteEvent.class, event -> {
					Command command = commands.get(event.getCommandName());
					if(command == null) return Mono.empty();
					else return command.autoComplete(event)
							.onErrorResume(err -> {
								logger.error("Error while executing command auto completion for " + event.getCommandName());
								return Mono.empty();
							});
				})
		)));
	}
	
}
