package de.doetsch.mensabot.data.types;

import discord4j.common.util.Snowflake;

public class GuildSettings {
	private final long guildId;
	private final int defaultCanteenId;
	private final long announcementChannelId;
	public GuildSettings(Long guildId, Integer defaultCanteenId, Long announcementChannelId){
		this.guildId = guildId;
		this.defaultCanteenId = defaultCanteenId;
		this.announcementChannelId = announcementChannelId;
	}
	public Snowflake getGuildId(){return Snowflake.of(guildId);}
	public int getDefaultCanteenId(){return defaultCanteenId;}
	public Snowflake getAnnouncementChannelId(){return Snowflake.of(announcementChannelId);}
}
