package de.doetsch.mensabot.canteen;

import de.doetsch.mensabot.data.types.GuildSettingsRepository;
import de.doetsch.mensabot.util.Util;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DailyMenuMessage {
	
	private static final Logger logger = LogManager.getLogger(DailyMenuMessage.class);
	
	public static Flux<Message> startScheduledMessages(GatewayDiscordClient client){
		return Flux.interval(Duration.between(Instant.now(), Util.getNextTime(20)).plus(Duration.ofSeconds(10)), Duration.ofDays(1))
				.flatMap(ignored -> client.getGuilds())
				.flatMap(guild -> sendMessage(client, guild, Instant.now().plus(10, ChronoUnit.HOURS))
						.onErrorResume(err -> {
							logger.error("Error while sending daily menu message for guild " + guild.getId().asString(), err);
							return Mono.empty();
						})
				);
	}
	
	private static Mono<Message> sendMessage(GatewayDiscordClient client, Guild guild, Instant date){
		return GuildSettingsRepository.findById(guild.getId().asLong())
				.flatMap(settings -> {
					if(settings.getAnnouncementChannelId().isEmpty()){
						logger.info("No announcement channel set for guild " + guild.getId().asString() + " - skipping announcement");
						return Mono.empty();
					}
					logger.info("Sending daily menu message for guild " + guild.getId().asString());
					return CanteenAPI.getCanteen(settings.getDefaultCanteenId())
							.flatMap(canteen -> Mono.zip(guild.getChannelById(settings.getAnnouncementChannelId().get()).ofType(MessageChannel.class), CanteenUtil.getMealsEmbed(canteen, date)))
							.flatMap(TupleUtils.function((channel, embed) -> channel.createMessage(embed)));
				});
	}
	
}
