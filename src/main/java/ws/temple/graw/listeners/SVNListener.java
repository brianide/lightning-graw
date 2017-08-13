package ws.temple.graw.listeners;

import java.util.regex.Pattern;

import com.google.api.client.repackaged.com.google.common.base.Objects;

import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import ws.temple.graw.GuildManager;
import ws.temple.graw.config.GuildConfig;
import ws.temple.graw.svn.SVNManager;
import ws.temple.graw.svn.SVNMonitor;

public class SVNListener extends PatternListener {

	private final SVNManager svnMan;
	private final GuildManager guildMan;
	
	public SVNListener(SVNManager manager, GuildManager guildMan) {
		this.svnMan = manager;
		this.guildMan = guildMan;
	}

	@Override
	public Pattern getPattern() {
		return Pattern.compile("^!svn(?: (\\d+|stat))?$");
	}

	@Override
	public void onMatch(String[] groups, MessageReceivedEvent event) {
		final String id = event.getGuild().getId();
		final GuildConfig config = guildMan.getConfig(id, false);
		final SVNMonitor monitor = svnMan.getMonitor(id);
		if(monitor != null && config != null && config.isResponsive()) {
			final TextChannel chan = event.getTextChannel();

			String reply;
			if(groups[0] == null) {
				reply = monitor.getLatestRevision();
			}
			else if(groups[0].equals("stat")) {
				reply = monitor.getStatusMessage();
			}
			else {
				final long rev = Long.parseLong(groups[0]);
				reply = Objects.firstNonNull(monitor.getRevision(rev), "r" + rev + " doesn't exist");
			}
			chan.sendMessageAsync(reply, null);
		}
	}
	
}
