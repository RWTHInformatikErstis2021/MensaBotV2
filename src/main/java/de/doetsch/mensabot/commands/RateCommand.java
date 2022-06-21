package de.doetsch.mensabot.commands;

import de.doetsch.mensabot.canteen.Canteen;
import de.doetsch.mensabot.canteen.CanteenAPI;
import de.doetsch.mensabot.canteen.CanteenUtil;
import de.doetsch.mensabot.canteen.Meal;
import de.doetsch.mensabot.data.types.Rating;
import de.doetsch.mensabot.data.types.RatingRepository;
import de.doetsch.mensabot.util.Util;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.AllowedMentions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RateCommand extends Command {
	private static final Logger logger = LogManager.getLogger(MensaCommand.class);
	
	private final ApplicationCommandRequest command = ApplicationCommandRequest.builder()
			.name("rate")
			.description("Bewerte ein Gericht des heutigen Tages")
			.addOption(ApplicationCommandOptionData.builder()
					.name("mensa")
					.description("Von welcher Mensa du ein Gericht bewerten willst")
					.type(4)
					.autocomplete(true)
					.required(false)
					.build()
			)
			.build();
	@Override
	ApplicationCommandRequest getCommand(){
		return command;
	}
	
	@Override
	Mono<Void> execute(ChatInputInteractionEvent event){
		int canteenId = event.getOption("mensa")
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(value -> (int)value.asLong())
				.orElse(Canteen.DefaultCanteen.ACADEMICA.getId());
		String dateSelectId = UUID.randomUUID().toString();
		return event.deferReply().withEphemeral(true).then(CanteenAPI.getCanteen(canteenId)
				.flatMap(Canteen::getMeals)
				.flatMapIterable(Map::entrySet)
				.map(entry -> Tuples.of(entry, Util.dateToDayDifference(entry.getKey())))
				.filter(t -> t.getT2() <= 0)
				.collectList()
				.flatMap(days -> {
					if(days.isEmpty()) return event.reply("Diese Mensa hatte kürzlich nicht geöffnet.").withEphemeral(true)
							.onErrorResume(err -> {
								logger.error("Error while responding to event", err);
								return Mono.empty();
							});
					return Mono.when(
							event.editReply(InteractionReplyEditSpec.builder()
									.contentOrNull("Von welchem Tag möchtest du ein Gericht bewerten?")
									.addComponent(ActionRow.of(SelectMenu.of(
											dateSelectId,
											days.stream()
													.sorted(Comparator.comparing(tuple -> tuple.getT1().getKey()))
													.map(tuple -> SelectMenu.Option.of(tuple.getT1().getKey() + " (" + Util.formatDayDifference(tuple.getT2()) + ")", tuple.getT1().getKey()))
													.collect(Collectors.toList())
									)))
									.build()
							).onErrorResume(err -> {
								logger.error("Error while editing date select message", err);
								return Mono.empty();
							}),
							event.getClient().on(SelectMenuInteractionEvent.class)
									.filter(newEvent -> newEvent.getCustomId().equals(dateSelectId))
									.next().take(Duration.ofMinutes(10))
									.flatMap(newEvent -> handleDateSelect(newEvent, days))
									.onErrorResume(err -> {
										logger.error("Error in date select handler", err);
										return Mono.empty();
									})
					);
				}));
	}
	
	private Mono<Void> handleDateSelect(SelectMenuInteractionEvent event, List<Tuple2<Map.Entry<String, List<Meal>>, Integer>> days){
		String date = event.getValues().get(0);
		List<Meal> meals = days.stream()
				.map(Tuple2::getT1)
				.filter(entry -> entry.getKey().equals(date))
				.map(Map.Entry::getValue)
				.findAny().orElse(List.of());
		if(meals.isEmpty()) return event.reply("An diesem Tag gab es keine Gerichte in der Mensa.").withEphemeral(true)
				.onErrorResume(err -> {
					logger.error("Error while responding to event", err);
					return Mono.empty();
				});
		String mealSelectId = UUID.randomUUID().toString();
		return event.deferReply().withEphemeral(true).then(Mono.when(
				event.editReply(InteractionReplyEditSpec.builder()
						.contentOrNull("Welches Gericht möchtest du bewerten?")
						.addComponent(ActionRow.of(SelectMenu.of(mealSelectId, meals.stream()
								.map(meal -> SelectMenu.Option.of(Util.trim(meal.category() + ": " + meal.name(), 100), Util.trim(meal.name(), 100)))
								.collect(Collectors.toList())
						)))
						.build()
				).onErrorResume(err -> {
					logger.error("Error while editing meal select message", err);
					return Mono.empty();
				}),
				event.getClient().on(SelectMenuInteractionEvent.class)
						.filter(newEvent -> newEvent.getCustomId().equals(mealSelectId))
						.next().take(Duration.ofMinutes(10))
						.flatMap(newEvent -> handleMealSelect(newEvent, meals))
						.onErrorResume(err -> {
							logger.error("Error in meal select handler", err);
							return Mono.empty();
						})
		));
	}
	
	private Mono<Void> handleMealSelect(SelectMenuInteractionEvent event, List<Meal> meals){
		String meal = event.getValues().get(0);
		// discord actually verifies select menus on their side
		//if(meals.stream().noneMatch(m -> Util.trim(m.name(), 100).equals(meal))) return Mono.empty();
		String buttonsBaseId = UUID.randomUUID().toString();
		return event.deferReply().withEphemeral(true).then(Mono.when(
				event.editReply(InteractionReplyEditSpec.builder()
						.contentOrNull("Wie viele Sterne gibst du dem Gericht?")
						.addComponent(ActionRow.of(
								Button.primary(buttonsBaseId + "1", "1"),
								Button.primary(buttonsBaseId + "2", "2"),
								Button.primary(buttonsBaseId + "3", "3"),
								Button.primary(buttonsBaseId + "4", "4"),
								Button.primary(buttonsBaseId + "5", "5")
						))
						.build()
				).onErrorResume(err -> {
					logger.error("Error while editing rating message", err);
					return Mono.empty();
				}),
				event.getClient().on(ButtonInteractionEvent.class)
						.filter(newEvent -> newEvent.getCustomId().startsWith(buttonsBaseId))
						.next().take(Duration.ofMinutes(10))
						.flatMap(newEvent -> handleRatingClick(newEvent, buttonsBaseId, meal))
						.onErrorResume(err -> {
							logger.error("Error in button click handler", err);
							return Mono.empty();
						})
		));
	}
	
	private Mono<Void> handleRatingClick(ButtonInteractionEvent event, String buttonBaseId, String meal){
		int rating = switch(event.getCustomId().substring(buttonBaseId.length())){
			case "1" -> 1;
			case "2" -> 2;
			case "3" -> 3;
			case "4" -> 4;
			case "5" -> 5;
			default -> -1;
		};
		if(rating == -1) return Mono.empty();
		return event.deferReply()
				.then(RatingRepository.save(new Rating(event.getInteraction().getUser().getId().asLong(), meal, rating)))
				.onErrorResume(err -> {
					logger.error("Error while saving rating", err);
					return event.editReply("Beim Speichern der Bewertung ist ein Fehler aufgetreten.")
							.onErrorResume(err2 -> {
								logger.error("Error beim Senden der Fehlernachricht", err2);
								return Mono.empty();
							})
							.then(Mono.empty());
				})
				.then(event.editReply(InteractionReplyEditSpec.builder()
						.allowedMentionsOrNull(AllowedMentions.suppressAll())
						.contentOrNull(meal + " wurde mit " + ":star:".repeat(rating) + " bewertet.")
						.build()
				))
				.onErrorResume(err -> {
					logger.error("Error while responding to created rating");
					return Mono.empty();
				}).then();
	}
	
	@Override
	public Mono<Void> autoComplete(ChatInputAutoCompleteEvent event){
		if(event.getFocusedOption().getName().equals("mensa")){
			String input = event.getFocusedOption().getValue()
					.map(ApplicationCommandInteractionOptionValue::getRaw)
					.orElse("");
			return CanteenAPI.getCanteens()
					.flatMapIterable(m->m)
					.map(canteen -> Tuples.of(canteen, CanteenUtil.scoreCanteenSearchMatch(canteen, input)))
					.sort(Comparator.comparingDouble(tuple -> -tuple.getT2()))
					.take(25)
					.filter(tuple -> tuple.getT2() > 0.3)
					.map(Tuple2::getT1)
					.map(canteen -> (ApplicationCommandOptionChoiceData)ApplicationCommandOptionChoiceData.builder().name(canteen.getName()).value(canteen.getId()).build())
					.collectList()
					.flatMap(event::respondWithSuggestions);
		}
		return Mono.empty();
	}
}
