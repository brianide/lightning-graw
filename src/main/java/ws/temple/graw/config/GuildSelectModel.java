package ws.temple.graw.config;

import java.util.List;

import ws.temple.graw.client.User;
import ws.temple.graw.client.UserGuild;

public class GuildSelectModel {
	
	private User user;
	private String clientID;
	private List<UserGuild> servers;
	
	public GuildSelectModel(User user, String clientID) {
		this.user = user;
		this.clientID = clientID;
	}
	
	public User getUser() {
		return user;
	}
	
	public GuildSelectModel setUser(User user) {
		this.user = user;
		return this;
	}
	
	public String getClientId() {
		return clientID;
	}

	public GuildSelectModel setClientId(String clientID) {
		this.clientID = clientID;
		return this;
	}

	public List<UserGuild> getServers() {
		return servers;
	}

	public GuildSelectModel setServers(List<UserGuild> servers) {
		this.servers = servers;
		return this;
	}

}
