package ws.temple.graw.svn;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import net.dv8tion.jda.JDA;
import ws.temple.graw.GuildConfigListener;
import ws.temple.graw.GuildStatusListener;
import ws.temple.graw.config.GuildConfig;
import ws.temple.graw.crypt.Crypter;
import ws.temple.graw.crypt.CrypterException;

public class SVNManager implements GuildConfigListener, GuildStatusListener {
	private static final Logger LOG = LoggerFactory.getLogger(SVNManager.class);
	
	/** Map associating SVNMonitor instances their respective guild IDs */
	private final Map<String,SVNMonitor> monitors = new ConcurrentHashMap<>();
	
	/** Discord API instance for channel resolution */
	private final JDA jda;
	
	/** Crypter instance for decrypting SVN passwords */
	private final Crypter crypt;
	
	private final SVNMonitorFactory factory;
	
	
	public SVNManager(JDA jda, Crypter crypt, SVNMonitorFactory factory) {
		this.jda = jda;
		this.crypt = crypt;
		this.factory = factory;
	}

	
	/**
	 * Returns the SVN manager associated with passed server ID, or null if
	 * none exists.
	 * 
	 * @param id
	 * @return
	 */
	public SVNMonitor getMonitor(String id) {
		return monitors.get(id);
	}
	
	
	public void applyConfig(SVNMonitor monitor, GuildConfig config) {
		try {
			monitor.stopMonitor();
			monitor.setRepository(SVNRepositoryFactory.create(SVNURL.parseURIEncoded(config.getRepoUrl())));
			monitor.setLogChannel(jda.getTextChannelById(config.getLogChannel()));
			monitor.setPollInterval(config.getQueryInterval());
			
			final char[] password = crypt.decrypt(config.getPassword(), "UTF-8");
			monitor.setAuthManager(SVNWCUtil.createDefaultAuthenticationManager(config.getUsername(), password));
			
			final SVNRevisionFormatter formatter = new SVNRevisionFormatter(config.getDateFormat(), config.getMessageFormat());
			monitor.setFormatter(formatter);
		}
		catch (CrypterException | SVNException | IOException e) {
			LOG.error("Exception while configuring monitor", e);
		}
	}
	
	
	public void shutdown() {
		for(SVNMonitor mon : monitors.values())
			mon.stopMonitor();
	}


	@Override
	public void onGuildActivated(String id, Optional<GuildConfig> config) {
		final SVNMonitor monitor = factory.createMonitor(id);
		monitors.put(id, monitor);
		config.ifPresent(c -> {
			applyConfig(monitor, c);
			monitor.startMonitor();
		});
	}


	@Override
	public void onGuildDeactivated(String id) {
		monitors.remove(id).stopMonitor();
	}


	@Override
	public void onConfigChange(String id, Optional<GuildConfig> oldConfig, GuildConfig newConfig) {
		final SVNMonitor monitor = monitors.get(id);
		applyConfig(monitor, newConfig);
		monitor.startMonitor();
	}

}
