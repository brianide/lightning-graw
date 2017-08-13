package ws.temple.graw;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.events.guild.GuildUnavailableEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import ws.temple.graw.config.GuildConfig;
import ws.temple.graw.db.QueryRunner;
import ws.temple.graw.db.dao.ConfigDAO;

public class GuildManager extends ListenerAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(GuildManager.class);
	
	/** QueryRunner for persistence */
	private final QueryRunner<ConfigDAO> runner;
	
	/** List of user IDs with super-administrative capacities */
	private final List<String> sadmins;
	
	/** Registry of listeners for configuration change events */
	private List<WeakReference<GuildConfigListener>> configListeners = Collections.synchronizedList(new LinkedList<>());
	
	/** Registry of listeners for server status change events */
	private List<WeakReference<GuildStatusListener>> statusListeners = Collections.synchronizedList(new LinkedList<>());
	
	private final JDA jda;
	private final String clientId;
	
	
	/**
	 * Creates and starts a new GrawBot instance.
	 * 
	 * @param api An initialized JDA object
	 * @param config
	 * @param crypt
	 * @throws InterruptedException 
	 * @throws IllegalArgumentException 
	 * @throws LoginException 
	 */
	public GuildManager(JDA jda, QueryRunner<ConfigDAO> runner, String clientId, String... sadmins) throws LoginException, IllegalArgumentException, InterruptedException {
		this.runner = runner;
		this.jda = jda;

		this.clientId = clientId;
		this.sadmins = Arrays.asList(sadmins);
	}
	
	
	public void initialize() {
		for(Guild guild : jda.getGuilds()) {
			registerServer(guild.getId());
		}
		
		jda.addEventListener(new GrawMaintenanceListener());
	}
	
	
	/**
	 * Returns the OAuth client ID associated with this bot.
	 * 
	 * @return
	 */
	public String getClientId() {
		return clientId;
	}
	

	/**
	 * Returns whether the user is authorized to manage the Graw configuration
	 * on the specified server.
	 * 
	 * @param guildId
	 * @param userId
	 * @param config
	 * @return
	 */
	public boolean checkManagementPermission(String guildId, String userId) {
		if(!sadmins.isEmpty() && sadmins.contains(userId))
			return true;
		
		else if(guildId !=  null) {
			final Guild guild = jda.getGuildById(guildId);
			if(guild.getOwnerId().equals(userId))
				return true;
			
			else {
				final GuildConfig config = getConfig(guildId, false);
				if(config != null) {
					final Role maintenance = guild.getRoleById(config.getMaintenanceRole());
					if(maintenance != null) {
						final User user = getDiscord().getUserById(userId);
						return guild.getRolesForUser(user).contains(maintenance);
					}
				}
			}
		}
		
		return false;
	}
	
	
	/**
	 * Returns whether or not the bot is running on the Guild identified by
	 * the passed ID.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isConnected(String id) {
		return jda.getGuilds()
				.stream()
				.map(Guild::getId)
				.anyMatch(gid -> gid.equals(id));
	}
	
	
	/**
	 * Returns the backing JDA API for this instance.
	 * 
	 * @return
	 */
	public JDA getDiscord() {
		return jda;
	}
	
	
	/**
	 * Returns the ServerConfig object corresponding to the passed Guild ID,
	 * or optionally creates a new one if none exists.
	 * 
	 * @param id The server ID to retrieve the configuration object for
	 * @param generate Whether to generate a new instance if none is found
	 * @return
	 */
	public GuildConfig getConfig(String id, boolean generate) {
		final GuildConfig config = runner.query(dao -> dao.getConfig(id));
		if(config != null)
			return config;
		else if(generate)
			return new GuildConfig();
		else
			return null;
	}
	
	
	/**
	 * Replace the Graw configuration for the specified server. This should
	 * ONLY be invoked with a configuration that has been validated.
	 * 
	 * @param id
	 * @param config
	 * @return
	 */
	public void updateConfig(String id, GuildConfig conf) {
		if(conf != null) {
			final Optional<GuildConfig> oldConf = runner.doTransaction(dao -> {
				final GuildConfig old = dao.getConfig(id);
				dao.putConfig(id, conf);
				return Optional.ofNullable(old);
			});
			
			Utils.fireListeners(configListeners, sl -> {
				sl.onConfigChange(id, oldConf, conf);
			});
			LOG.info("Updated configuration for server: {}({})", jda.getGuildById(id).getName(), id);
		}
	}
	
	
	/**
	 * Activate a server.
	 * 
	 * @param id
	 * @return
	 */
	private void registerServer(String id) {
		final GuildConfig conf = getConfig(id, false);
		Utils.fireListeners(statusListeners, sl -> {
			sl.onGuildActivated(id, Optional.ofNullable(conf));
		});
		
		if(conf != null) {
			LOG.debug("Loaded configuration object: {}", conf);
			LOG.info("Configured server activated: {}({})", jda.getGuildById(id).getName(), id);
		}
		else {
			LOG.info("Unconfigured server activated: {}({})", jda.getGuildById(id).getName(), id);
		}
	}

	
	/**
	 * Deactivate a server.
	 * 
	 * @param id
	 * @return
	 */
	private GuildManager unregisterServer(String id) {
		Utils.fireListeners(statusListeners, sl -> {
			sl.onGuildDeactivated(id);
		});
		
		LOG.info("Unregistered server: {}({})", jda.getGuildById(id).getName(), id);
		return this;
	}
	
	
	/**
	 * Registers a listener for configuration change events.
	 * 
	 * @param listener
	 */
	public void addConfigListener(GuildConfigListener listener) {
		configListeners.add(new WeakReference<GuildConfigListener>(listener));
	}
	
	
	/**
	 * Registers a listener for server status change events.
	 * 
	 * @param listener
	 */
	public void addStatusListener(GuildStatusListener listener) {
		statusListeners.add(new WeakReference<GuildStatusListener>(listener));
	}
	

	/**
	 * Performs any maintenance actions necessary to the state of the GrawBot
	 * instance in response to Discord events.
	 *
	 */
	private class GrawMaintenanceListener extends ListenerAdapter {
		@Override
		public void onGuildAvailable(GuildAvailableEvent event) {
			final String id = event.getGuild().getId();
			registerServer(id);
		}

		@Override
		public void onGuildUnavailable(GuildUnavailableEvent event) {
			final String id = event.getGuild().getId();
			unregisterServer(id);
		}
	}
	
	// TODO this belongs in the context builder
	public void shutdown() {
		jda.shutdown();
	}
	
}
