package de.doetsch.mensabot.canteen;

import de.doetsch.mensabot.util.Util;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CanteenUtil {
	
	private static final Logger logger = LogManager.getLogger(CanteenUtil.class);
	
	private static final List<Tuple2<String, String>> mealEmojis = List.of(
			Tuples.of("schnitzel", "<:schnitzel:943559144135336047>"),
			Tuples.of("burger", ":hamburger:"),
			Tuples.of("pizza", ":pizza:"),
			Tuples.of("bbq drumsticks", ":poultry_legt:"),
			Tuples.of("pfannkuchen", ":pancakes:"),
			Tuples.of("kuchen", ":cake:"),
			Tuples.of("küchlein", ":cake:"),
			Tuples.of("spaghetti", ":spaghetti:"),
			Tuples.of("penne rigate", ":spaghetti:"),
			Tuples.of("suppe", ":stew:"),
			Tuples.of("keule", ":poultry_leg:"),
			Tuples.of("steak", ":cut_of_meat:"),
			Tuples.of("hähnchen", ":chicken:"),
			Tuples.of("huhn", ":chicken:"),
			Tuples.of("fisch", ":fish:"),
			Tuples.of("lachs", ":fish:"),
			Tuples.of("reis", ":rice:"),
			//Tuples.of("kürbis", ":pumpkin:"), wieso gibt es kein kürbis emoj??
			Tuples.of("pommes", ":fries:"),
			Tuples.of("apfel", ":apple:"),
			Tuples.of("dumpling", ":dumpling:"),
			Tuples.of("shrimp", ":fried_shrimp:"),
			Tuples.of("brokkoli", ":broccoli:"),
			Tuples.of("paprika", ":bell_pepper:"),
			Tuples.of("mais", ":corn:"),
			Tuples.of("karotte", ":carrot:"),
			Tuples.of("möhrchen", ":carrot:"),
			Tuples.of("kartoffel", ":potato:"),
			Tuples.of("salat", ":salad:"),
			Tuples.of("eintopf", ":stew:"),
			Tuples.of("käse", ":cheese:"),
			Tuples.of("speck", ":bacon:"),
			Tuples.of("zwiebel", ":onion:"),
			Tuples.of("chili", ":hot_pepper:"),
			Tuples.of("scharf", ":hot_pepper:"),
			Tuples.of("knoblauch", ":garlic:")
	);
	private static final Map<String, String> categoryEmojis = Map.ofEntries(
			Map.entry("tellergericht", ":bread:"),
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
		return mealEmojis.stream().filter(tuple -> name.contains(tuple.getT1())).map(Tuple2::getT2).findFirst().orElseGet(()->
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
	public static String formatRating(Meal.Rating rating){
		int full = (int)rating.getRating();
		double remaining = rating.getRating() - full;
		return ":star:".repeat(full) + ratingEmojis.stream().filter(entry -> remaining >= entry.getKey()).findFirst().map(Map.Entry::getValue).orElse("") + "(" + rating.getRatingCount() + ")";
	}
	
	public static String formatPrice(double price){
		return String.format("%.2f€", price);
	}
	
	public static Mono<EmbedCreateSpec> getMealsEmbed(Canteen canteen, Instant date){
		return canteen.getMeals(date)
				.flatMap(meals -> Flux.fromIterable(meals)
						.flatMapSequential(meal -> meal.getRating()
								.map(rating -> Tuples.of(meal, Optional.of(rating)))
								.defaultIfEmpty(Tuples.of(meal, Optional.empty()))
						)
						.collectList()
				)
				.map(meals -> EmbedCreateSpec.builder()
						.title("Gerichte in " + canteen.getName())
						.description(Util.formatHumanReadableDate(date) + " (" + Util.formatDayDifference(Util.dateToDayDifference(date)) + ")")
						.addAllFields(meals.stream().map(TupleUtils.function((meal, rating) -> {
							String mealTitle = getEmojiForMeal(meal) + " " + meal.name();
							if(rating.isPresent()) mealTitle += " " + formatRating(rating.get());
							String mealDescription = meal.category();
							double price = meal.getStudentPrice();
							if(price > 0){
								mealDescription += "\n" + formatPrice(price);
								double othersPrice = meal.getOthersPrice();
								if(othersPrice > 0 && othersPrice != price) mealDescription += " (" + formatPrice(othersPrice) + ")";
							}
							boolean shouldInline = !(meal.category().equalsIgnoreCase("hauptbeilagen") || meal.category().equalsIgnoreCase("nebenbeilage"));
							return EmbedCreateFields.Field.of(mealTitle, mealDescription, shouldInline);
						})).collect(Collectors.toList()))
						.build()
				)
				.defaultIfEmpty(EmbedCreateSpec.builder()
						.title(canteen.getName() + " ist geschlossen.")
						.build()
				);
	}
	
	public static double scoreCanteenSearchMatch(Canteen canteen, String search){
		String canteenName = canteen.getName().toLowerCase();
		String canteenCity = canteen.getCity().toLowerCase();
		return scoreSearchMatch(search.toLowerCase(), canteenName, canteenCity);
	}
	
	public static double scoreMealSearchMatch(Meal meal, String search){
		String mealName = meal.name().toLowerCase();
		String mealCategory = meal.category().toLowerCase();
		return scoreSearchMatch(search.toLowerCase(), mealName, mealCategory);
	}
	
	private static final Pattern whitespace = Pattern.compile(" ");
	private static double scoreSearchMatch(String search, String name, String secondary){
		if(name.equals(search)) return 1;
		String[] searchParts = whitespace.split(search);
		List<String> nameParts = List.of(whitespace.split(name));
		double score = 0;
		for(String part : searchParts){
			if(secondary.equals(part)) score += 1.5 / searchParts.length;
			else if(nameParts.contains(part)) score += 1.0 / searchParts.length;
			else if(nameParts.stream().anyMatch(namePart -> namePart.contains(part))) score += 0.8 / searchParts.length;
			else if(secondary.contains(search)) score += 0.5 / searchParts.length;
			else score -= 0.9 / searchParts.length;
		}
		return score;
	}
	
}
