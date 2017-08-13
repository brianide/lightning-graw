package ws.temple.graw.svn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;

import net.dv8tion.jda.entities.TextChannel;
import ws.temple.graw.Utils;
import ws.temple.graw.db.QueryRunner;
import ws.temple.graw.db.dao.ConfigDAO;

public class SVNMonitor {
	private static final Logger LOG = LoggerFactory.getLogger(SVNMonitor.class);
	
	private static final String ALERT_CONNECTION_RESUMED = "Repository connection restored";
	private static final String ALERT_CONNECTION_LOST = "Could not esbalish connection to repository";
	private static final String ALERT_CREDENTIALS_REJECTED = "Credentials rejected by repository";
	
	private static final String STATE_NORMAL = "Operating normally";
	private static final String STATE_NO_CONNECTION = "Unable to connect to repository";
	private static final String STATE_NOT_CONFIGURED = "This server is not configured";
	private static final String STATE_CREDENTIALS_REJECTED = "The configured repository credentials are being rejected";
	private static final String STATE_MYSTERIOUS = "I don't know what's going on right now to be honest";
	
	/** ID for the server to which this monitor reports */
	private final String guildId;
	
	/** Executor reference for scheduling the polling task */
	private final ScheduledExecutorService exec;
	
	/** QueryRunner for persisting known revision number */
	private final QueryRunner<ConfigDAO> runner;

	/** Handle to repository polling task */
	private Future<?> pollTask = null;

	/* Self-explanatory configuration parameters */
	private SVNRepository repo;
	private SVNRevisionFormatter formatter;
	private TextChannel channel;
	private long pollInterval;
	
	/** Tracks this monitor's operational status */
	private SVNStatus state = SVNStatus.NOT_CONFIGURED;
	
	/** Tracks the most recent revision this monitor is aware of */
	private long lastRev = -1;
	

	
	public SVNMonitor(String id, QueryRunner<ConfigDAO> runner, ScheduledExecutorService exec) {
		this.guildId = id;
		this.runner = runner;
		this.exec = exec;
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Task Management
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	/**
	 * Initiate the repository monitoring task.
	 * 
	 */
	public void startMonitor() {
		announceStatusChange(SVNStatus.NORMAL);
		if(pollTask == null && channel != null) {
			lastRev = runner.query(dao -> dao.getLatestRevision(guildId));
			pollTask = exec.scheduleWithFixedDelay(this::pollRepository, 0, pollInterval, TimeUnit.SECONDS);
		}
	}
	
	
	/**
	 * Halt the repository monitoring task.
	 * 
	 */
	public void stopMonitor() {
		if(pollTask != null) {
			Utils.blockCancel(pollTask);
			pollTask = null;
		}
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Revision Querying
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	/**
	 * Returns the specified revision's information, formatted according to 
	 * this monitor's SVNRevisionFormatter.
	 * 
	 * @param revId
	 * @return
	 * @throws SVNException
	 * @throws IOException
	 */
	private String getFormattedRevision(long revId) throws SVNException {
		final SVNProperties props = repo.getRevisionProperties(revId, null);
		
		try {
			return formatter.format(revId, props);
		}
		catch (IOException e) {
			LOG.error("Exception while formatting revision (what the fuck?)", e);
		}
		return null;
	}
	
	
	/**
	 * Returns the specified range of revisions, formatted according to this
	 * monitor's SVNRevisionFormatter.
	 * 
	 * @param start
	 * @param end
	 * @return
	 * @throws SVNException
	 * @throws IOException
	 */
	private List<String> getFormattedRevisions(long start, long end) throws SVNException {
		final List<String> revs = new ArrayList<>((int) (end - start));
		for(long i = start; i <= end; i++) {
			revs.add(getFormattedRevision(i));
		}
		return revs;
	}
	
	
	/**
	 * Returns the specified revision's information, formatted according to
	 * this monitor's SVNRevisionFormatter, if it exists and the repository
	 * could be successfully contacted. Returns null otherwise.
	 * 
	 * @param id
	 * @return
	 */
	public String getRevision(long id) {
		if(getStatus() == SVNStatus.NORMAL) {
			try {
				if(id > 0 && id <= repo.getLatestRevision())
					return getFormattedRevision(id);
			}
			catch (SVNException e) {
				LOG.error("Exception while querying for revision", e);
			}
		}
		return null;
	}
	
	
	/**
	 * Returns the latest revision, formatted according to this monitor's
	 * formatter.
	 * 
	 * @return
	 */
	public String getLatestRevision() {
		try {
			return getRevision(repo.getLatestRevision());
		}
		catch (SVNException e) {
			LOG.error("Exception while querying for revision", e);
		}
		return null;
	}
	
	
	/**
	 * Query for new revisions and dispatch notifications.
	 * 
	 */
	private void pollRepository() {
		if(getStatus() == SVNStatus.NORMAL) {
			try {
				final long latestRevision = repo.getLatestRevision();
				
				if(lastRev > 0 && lastRev < latestRevision) {
					for(String rev : getFormattedRevisions(lastRev + 1, latestRevision))
						channel.sendMessageAsync(rev, null);
				}
				
				lastRev = latestRevision;
				runner.executeTransaction(dao -> dao.updateLatestRevision(guildId, lastRev));
			}
			catch(SVNException e) {
				LOG.error("Exception while querying revisions", e);
			}
		}
	}
	
	
	/**
	 * Updates the current status of the repository connection. Dispatches a
	 * notification if the status was changed.
	 * 
	 * @param newState
	 * @return The new state
	 */
	private SVNStatus announceStatusChange(SVNStatus newState) {
		if(state != newState) {
			if(newState == SVNStatus.NORMAL && state != SVNStatus.NOT_CONFIGURED)
				channel.sendMessageAsync(ALERT_CONNECTION_RESUMED, null);
			
			else if(newState == SVNStatus.NO_CONNECTION)
				channel.sendMessageAsync(ALERT_CONNECTION_LOST, null);
			
			else if(newState == SVNStatus.BAD_CREDENTIALS)
				channel.sendMessageAsync(ALERT_CREDENTIALS_REJECTED, null);
		}
		state = newState;
		return state;
	}
	
	
	/**
	 * Get the monitor's current status.
	 * 
	 * @return
	 */
	public SVNStatus getStatus() {
		try {
			repo.testConnection();
			return announceStatusChange(SVNStatus.NORMAL);
		}
		catch(SVNAuthenticationException e) {
			return announceStatusChange(SVNStatus.BAD_CREDENTIALS);
		}
		catch (SVNException e) {
			return announceStatusChange(SVNStatus.NO_CONNECTION);
		}
	}
	
	
	/**
	 * Get the monitor's current status as a String.
	 * 
	 * @return
	 */
	public String getStatusMessage() {
		if(state == SVNStatus.NORMAL)
			return STATE_NORMAL;
		else if(state == SVNStatus.NO_CONNECTION)
			return STATE_NO_CONNECTION;
		else if(state == SVNStatus.BAD_CREDENTIALS)
			return STATE_CREDENTIALS_REJECTED;
		else if(state == SVNStatus.NOT_CONFIGURED)
			return STATE_NOT_CONFIGURED;
		else
			return STATE_MYSTERIOUS;
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Setters
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public SVNMonitor setRepository(SVNRepository repo) {
		this.repo = repo;
		return this;
	}
	
	public SVNMonitor setAuthManager(ISVNAuthenticationManager auth) {
		if(repo == null)
			throw new IllegalStateException();
		repo.setAuthenticationManager(auth);
		return this;
	}
	
	public SVNMonitor setFormatter(SVNRevisionFormatter formatter) {
		this.formatter = formatter;
		return this;
	}
	
	public SVNMonitor setLogChannel(TextChannel channel) {
		this.channel = channel;
		return this;
	}
	
	public SVNMonitor setPollInterval(long interval) {
		this.pollInterval = interval;
		return this;
	}
	
}
