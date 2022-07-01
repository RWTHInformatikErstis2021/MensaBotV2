package de.doetsch.mensabot.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class Util {
	
	private static final Logger logger = LogManager.getLogger(Util.class);
	
	private static final ZoneId timezone = ZoneId.systemDefault();
	
	public static String formatDate(Instant instant){
		ZonedDateTime date = instant.atZone(timezone);
		return pad(date.getYear(), 4) + "-" + pad(date.getMonthValue(), 2) + "-" + pad(date.getDayOfMonth(), 2);
	}
	
	public static String formatHumanReadableDate(Instant instant){
		ZonedDateTime date = instant.atZone(timezone);
		return date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN));
	}
	
	public static int dateToDayDifference(String date){
		LocalDate day = LocalDate.parse(date, DateTimeFormatter.ISO_DATE.withZone(timezone));
		LocalDate now = LocalDate.now(timezone);
		return Period.between(now, day).getDays();
	}
	
	public static int dateToDayDifference(Instant date){
		LocalDate day = LocalDate.ofInstant(date, timezone);
		LocalDate now = LocalDate.now(timezone);
		return Period.between(now, day).getDays();
	}
	
	public static String formatDayDifference(int difference){
		return switch(difference){
			case -2 -> "vorgestern";
			case -1 -> "gestern";
			case 0 -> "heute";
			case 1 -> "morgen";
			case 2 -> "übermorgen";
			default -> difference < 0 ? "vor " + (-difference) + " Tagen" : "in " + difference + " Tagen";
		};
	}
	
	public static Instant getNextTime(int hour){
		ZonedDateTime now = ZonedDateTime.now(timezone);
		ZonedDateTime next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
		if(now.isAfter(next))
			next = next.plusDays(1);
		return next.toInstant();
	}
	
	public static String pad(int number, int length){
		String s = Integer.toString(number);
		return "0".repeat(Math.max(0, length - s.length())) + s;
	}
	
	/**
	 * Caches a {@link Mono} for the given duration, but at most for the current day
	 * @param toCache           the {@link Mono} to cache
	 * @param cacheDuration     the amount of how long the {@link Mono} should get cached
	 * @param cacheDurationUnit the unit of how long the {@link Mono} should get cached
	 * @return the cached {@link Mono}
	 */
	public static <T> Mono<T> cacheForDay(Mono<T> toCache, int cacheDuration, TemporalUnit cacheDurationUnit){
		AtomicReference<Instant> lastRequest = new AtomicReference<>(Instant.EPOCH);
		return toCache.cacheInvalidateIf(t -> {
			Instant now = Instant.now();
			Instant lr = lastRequest.get();
			if(lr.isBefore(now.minus(cacheDuration, cacheDurationUnit)) || !formatDate(lr).equals(formatDate(now))){
				lastRequest.set(now);
				return true;
			}
			return false;
		});
	}
	
	public static String trim(String text, int maxLength){
		return text.length() <= maxLength ? text : text.substring(0, maxLength);
	}
	
}
