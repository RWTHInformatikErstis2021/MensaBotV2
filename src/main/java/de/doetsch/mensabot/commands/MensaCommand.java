package de.doetsch.mensabot.commands;

import de.doetsch.mensabot.canteen.Canteen;
import de.doetsch.mensabot.canteen.CanteenAPI;
import de.doetsch.mensabot.canteen.CanteenUtil;
import de.doetsch.mensabot.util.Util;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MensaCommand extends Command {
	
	private static final Logger logger = LogManager.getLogger(MensaCommand.class);
	
	private final ApplicationCommandRequest command = ApplicationCommandRequest.builder()
			.name("mensa")
			.description("Gibt den Speiseplan der gewählten Mensa aus")
			.options(List.of(
					ApplicationCommandOptionData.builder()
							.name("mensa")
							.description("Die Mensa, von welcher der Speiseplan ausgegeben werden soll")
							.type(4)
							.required(false)
							.choices(Stream.of(Canteen.DefaultCanteen.values())
									.map(canteen -> ApplicationCommandOptionChoiceData.builder()
											.name(canteen.getDisplayName())
											.value(canteen.getId())
											.build()
									)
									.collect(Collectors.toList())
							)
							.build(),
					ApplicationCommandOptionData.builder()
							.name("tag")
							.description("Der Tag, von dem der Speiseplan ausgegeben werden soll")
							.type(4)
							.required(false)
							.autocomplete(true)
							.build()
			))
			.build();
	
	@Override
	ApplicationCommandRequest getCommand(){
		return command;
	}
	
	@Override
	public Mono<Void> execute(ChatInputInteractionEvent event){
		return event.getInteraction().getCommandInteraction().map(interaction -> {
			int canteenId = interaction.getOption("mensa")
					.flatMap(ApplicationCommandInteractionOption::getValue)
					.map(value -> (int)value.asLong())
					.orElse(Canteen.DefaultCanteen.ACADEMICA.getId());
			Instant day = Instant.now().plus(interaction.getOption("tag")
					.flatMap(ApplicationCommandInteractionOption::getValue)
					.map(value -> (int)value.asLong())
					.orElse(0), ChronoUnit.DAYS);
			return CanteenAPI.getCanteen(canteenId)
					.flatMap(canteen -> CanteenUtil.getMealsEmbed(canteen, day))
					.flatMap(embedSpec -> event.reply(InteractionApplicationCommandCallbackSpec.builder()
							.addEmbed(embedSpec)
							.build()
					));
		}).orElse(event.reply("Fehler beim Aufrufen des Befehls."));
	}
	
	@Override
	public Mono<Void> autoComplete(ChatInputAutoCompleteEvent event){
		if(event.getFocusedOption().getName().equals("tag")){
			String input = event.getFocusedOption().getValue()
					.map(ApplicationCommandInteractionOptionValue::getRaw)
					.orElse("");
			int canteenId = event.getOption("canteen")
					.flatMap(ApplicationCommandInteractionOption::getValue)
					.map(value -> (int)value.asLong())
					.orElse(Canteen.DefaultCanteen.ACADEMICA.getId());
			Map<String, Integer> dates = new HashMap<>();
			dates.put("gestern", -1);
			dates.put("heute", 0);
			dates.put("morgen", 1);
			dates.put("übermorgen", 2);
			return CanteenAPI.getCanteen(canteenId)
					.flatMap(Canteen::getMeals)
					.flatMapIterable(Map::keySet)
					.sort()
					.map(date -> Map.entry(date, Util.dateToDayDifference(date)))
					.mergeWith(Flux.fromIterable(dates.entrySet()))
					.filter(entry -> entry.getKey().startsWith(input))
					.take(25)
					.map(entry -> ApplicationCommandOptionChoiceData.builder()
							.name(entry.getKey())
							.value(entry.getValue())
							.build()
					)
					.collectList()
					.flatMap(options -> event.respondWithSuggestions(new ArrayList<>(options)));
		}
		return Mono.empty();
	}
}
