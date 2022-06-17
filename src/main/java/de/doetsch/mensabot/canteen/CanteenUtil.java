package de.doetsch.mensabot.canteen;

import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CanteenUtil {
	
	private static final Map<String, String> mealEmojis = Map.ofEntries(
			Map.entry("schnitzel", "<:schnitzel:943559144135336047>"),
			Map.entry("burger", ":hamburger:"),
			Map.entry("pizza", ":pizza:"),
			Map.entry("pfannkuchen", ":pancakes:"),
			Map.entry("kuchen", ":cake:"),
			Map.entry("küchlein", ":cake:"),
			Map.entry("spaghetti", ":spaghetti:"),
			Map.entry("suppe", ":stew:"),
			Map.entry("chili", ":hot_pepper:"),
			Map.entry("scharf", ":hot_pepper:"),
			Map.entry("keule", ":poultry_leg:"),
			Map.entry("steak", ":cut_of_meat:"),
			Map.entry("hähnchen", ":chicken:"),
			Map.entry("huhn", ":chicken:"),
			Map.entry("fisch", ":fish:"),
			Map.entry("lachs", ":fish:"),
			Map.entry("reis", ":rice:"),
			Map.entry("pommes", ":fries:"),
			Map.entry("apfel", ":apple:"),
			Map.entry("brokkoli", ":broccoli:"),
			Map.entry("paprika", ":bell_pepper:"),
			Map.entry("mais", ":corn:"),
			Map.entry("karotte", ":carrot:"),
			Map.entry("kartoffel", ":potato:"),
			Map.entry("salat", ":salad:"),
			Map.entry("eintopf", ":stew:"),
			Map.entry("käse", ":cheese:"),
			Map.entry("zwiebel", ":onion:")
	);
	private static final Map<String, String> categoryEmojis = Map.ofEntries(
			Map.entry("vegetarisch", ":leafy_green:"),
			Map.entry("klassiker", ":cut_of_meat:"),
			Map.entry("burger der woche", ":hamburger:"),
			Map.entry("burger classics", ":hamburger:"),
			Map.entry("hauptbeilagen", ":potato:"),
			Map.entry("nebenbeilagen", ":salad:")
	);
	public static String getEmojiForMeal(Meal meal){
		String name = meal.name().toLowerCase();
		String category = meal.category().toLowerCase();
		return mealEmojis.entrySet().stream().filter(entry -> name.contains(entry.getKey())).map(Map.Entry::getValue).findFirst().orElseGet(()->
				categoryEmojis.entrySet().stream().filter(entry -> category.contains(entry.getKey())).map(Map.Entry::getValue).findFirst().orElse(":fork_knife_plate:")
		);
	}
	
	private static final List<Map.Entry<Double, String>> ratingEmojis = List.of(
			Map.entry(0.9, ""),
			Map.entry(0.8, ""),
			Map.entry(0.7, ""),
			Map.entry(0.6, ""),
			Map.entry(0.5, ""),
			Map.entry(0.4, ""),
			Map.entry(0.3, ""),
			Map.entry(0.2, ""),
			Map.entry(0.1, "")
	);
	public static String formatRating(double rating, int ratingCount){
		int full = (int)rating;
		double remaining = rating - full;
		return ":star:".repeat(full) + ratingEmojis.stream().filter(entry -> remaining >= entry.getKey()).findFirst().map(Map.Entry::getValue).orElse("") + "(" + ratingCount + ")";
	}
	
	public static Mono<EmbedCreateSpec> getMealsEmbed(Canteen canteen, Instant date){
		return canteen.getMeals(date).map(meals -> {
			// TODO implement
			return EmbedCreateSpec.builder().build();
		}).defaultIfEmpty(EmbedCreateSpec.builder()
				.title(canteen.getName() + " ist geschlossen.")
				.build()
		);
	}
	
}
