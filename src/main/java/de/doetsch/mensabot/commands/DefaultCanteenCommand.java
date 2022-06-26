package de.doetsch.mensabot.commands;

import de.doetsch.mensabot.canteen.CanteenAPI;
import de.doetsch.mensabot.canteen.CanteenUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.AllowedMentions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;

public class DefaultCanteenCommand extends Command {
	
	private static final Logger logger = LogManager.getLogger(DefaultCanteenCommand.class);
	
	private final ApplicationCommandRequest command = ApplicationCommandRequest.builder()
			.name("standard_mensa")
			.description("Wähle die Standardmensa für diesen Server")
			.defaultPermission(false) // TODO switch to defaultMemberPermission when supported by D4J
			.options(List.of(
					ApplicationCommandOptionData.builder()
							.name("mensa")
							.description("Die Mensa, die standardmäßig ausgewählt werden soll, wenn keine angegeben ist")
							.type(4)
							.required(true)
							.autocomplete(true)
							.build()
			))
			.build();
	@Override
	ApplicationCommandRequest getCommand(){
		return command;
	}
	
	@Override
	Mono<Void> execute(ChatInputInteractionEvent event, int defaultCanteenId){
		int canteenId = event.getOption("mensa")
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(value -> (int)value.asLong())
				.orElse(defaultCanteenId);
		long guildId = event.getInteraction().getGuildId().map(Snowflake::asLong).orElse(0L);
		return event.deferReply().then(CanteenAPI.getCanteen(canteenId))
				.flatMap(canteen -> CanteenUtil.setDefaultCanteenId(guildId, canteenId)
						.then(event.editReply(InteractionReplyEditSpec.builder()
								.contentOrNull("Standardmensa auf " + canteen.getName() + " gesetzt")
								.allowedMentionsOrNull(AllowedMentions.suppressAll())
								.build()
						))
						.onErrorResume(err -> {
							logger.error("Error setting default canteen id of guild " + guildId + " to " + canteenId, err);
							return event.editReply("Beim Setzen der Standardmensa ist ein Fehler aufgetreten")
									.onErrorResume(err2 -> {
										logger.error("Error editing reply to failure message", err2);
										return Mono.empty();
									});
						})
				)
				.switchIfEmpty(event.editReply("Die angegebene Mensa konte nicht gefunden werden")
						.onErrorResume(err -> {
							logger.error("Error editing reply to canteen not found message", err);
							return Mono.empty();
						})
				)
				.then();
	}
	
	@Override
	public Mono<Void> autoComplete(ChatInputAutoCompleteEvent event, int defaultCanteenId){
		if(event.getFocusedOption().getName().equals("mensa")){
			String input = event.getFocusedOption().getValue()
					.map(ApplicationCommandInteractionOptionValue::getRaw)
					.orElse("");
			return CanteenUtil.getCanteenAutoCompleteOptions(input)
					.flatMap(event::respondWithSuggestions);
		}
		return Mono.empty();
	}
}
