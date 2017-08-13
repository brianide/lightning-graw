package ws.temple.graw.config;

import java.util.List;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import ws.temple.graw.client.User;

public class BotConfigModel {

	private User user;
	private Guild server;
	private List<TextChannel> channels;
	private List<Role> roles;
	private String csrfToken;
	private GuildConfig config;
	
	public BotConfigModel(User user, Guild server, GuildConfig config) {
		this.user = user;
		this.server = server;
		this.config = config;
	}
	
	public User getUser() {
		return user;
	}
	
	public BotConfigModel setUser(User user) {
		this.user = user;
		return this;
	}
	
	public Guild getServer() {
		return server;
	}
	
	public BotConfigModel setServer(Guild server) {
		this.server = server;
		return this;
	}
	
	public List<TextChannel> getChannels() {
		return channels;
	}
	
	public BotConfigModel setChannels(List<TextChannel> channels) {
		this.channels = channels;
		return this;
	}
	
	public List<Role> getRoles() {
		return roles;
	}
	
	public BotConfigModel setRoles(List<Role> roles) {
		this.roles = roles;
		return this;
	}
	
	public String getCsrfToken() {
		return csrfToken;
	}

	public BotConfigModel setCsrfToken(String csrfToken) {
		this.csrfToken = csrfToken;
		return this;
	}

	public GuildConfig getConfig() {
		return config;
	}
	
	public BotConfigModel setConfig(GuildConfig config) {
		this.config = config;
		return this;
	}
	
}
