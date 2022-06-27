package de.doetsch.mensabot.commands;

import de.doetsch.mensabot.data.types.GuildSettingsRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;

public class AnnouncementChannelCommand extends Command {
	
	private static final Logger logger = LogManager.getLogger(AnnouncementChannelCommand.class);
	
	private final ApplicationCommandRequest command = ApplicationCommandRequest.builder()
			.name("announcement_channel")
			.description("Setze den Kanal, in dem täglich der neue Speiseplan ausgegeben werden soll")
			.defaultPermission(false) // TODO switch to defaultMemberPermission when supported by D4J
			.addOption(ApplicationCommandOptionData.builder()
					.name("channel")
					.description("Diesen Parameter auszulassen deaktiviert automatisierte Speisepläne")
					.type(ApplicationCommandOption.Type.CHANNEL.getValue())
					.required(false)
					.channelTypes(List.of(Channel.Type.GUILD_TEXT.getValue(), Channel.Type.GUILD_NEWS.getValue()))
					.build()
			)
			.build();
	@Override
	ApplicationCommandRequest getCommand(){
		return command;
	}
	
	@Override
	Mono<Void> execute(ChatInputInteractionEvent event, int defaultCanteenId){
		long guildId = event.getInteraction().getGuildId().map(Snowflake::asLong).orElse(0L);
		return event.deferReply()
				.then(event.getOption("channel")
						.flatMap(ApplicationCommandInteractionOption::getValue)
						.map(ApplicationCommandInteractionOptionValue::asChannel)
						.orElse(Mono.empty())
						.ofType(GuildMessageChannel.class)
						.zipWhen(channel -> channel.getEffectivePermissions(event.getClient().getSelfId()))
						.flatMap(TupleUtils.function((channel, permissions) -> {
							if(!permissions.contains(Permission.SEND_MESSAGES))
								return event.editReply("Ich kann in " + channel.getMention() + " nicht schreiben. Bitte gib mir Berechtigung dazu und führ den Befehl erneut aus, oder wähle einen anderen Kanal.");
							return GuildSettingsRepository.setAnnouncementChannelId(guildId, channel.getId().asLong())
									.then(event.editReply("Der Speiseplan wird nun in " + channel.getMention() + " ausgegeben.")
											.onErrorResume(err -> {
												logger.error("Error while editing reply after announcement channel set", err);
												return Mono.empty();
											})
									)
									.onErrorResume(err -> {
										logger.error("Error while setting announcement channel", err);
										return event.editReply("Fehler beim Setzen des Announcement Channels.")
												.onErrorResume(err2 -> {
													logger.error("Error while editing reply after announcement channel set error", err2);
													return Mono.empty();
												});
									});
						}))
						.switchIfEmpty(GuildSettingsRepository.setAnnouncementChannelId(guildId, null)
								.then(event.editReply("Der Speiseplan wird nun nicht mehr automatisch ausgegeben."))
								.onErrorResume(err -> {
									logger.error("Error while setting announcement channel for guild " + guildId + " to null", err);
									return event.editReply("Fehler beim Setzen des Announcement Channels.")
											.onErrorResume(err2 -> {
												logger.error("Error while editing reply after announcement channel set error", err2);
												return Mono.empty();
											});
								})
						)
				).then();
	}
}
