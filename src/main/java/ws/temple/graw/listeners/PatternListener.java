package ws.temple.graw.listeners;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public abstract class PatternListener extends ListenerAdapter {

	/**
	 * Specifies the pattern to match against. The return value of this method
	 * will be cached upon construction.
	 * 
	 * @return
	 */
	public abstract Pattern getPattern();
	
	
	/**
	 * Defines the listener's response to a pattern match.
	 * 
	 * @param groups The groups of the regex match, indexed from 0.
	 * @param event
	 */
	public abstract void onMatch(String[] groups, MessageReceivedEvent event);
	
	
	
	private final Pattern pattern;
	
	public PatternListener() {
		pattern = getPattern();
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		check(event.getMessage().getContent(), event);
	}
	
	public void check(String content, MessageReceivedEvent event) {
		final Matcher m = pattern.matcher(content);
		if(m.find()) {
			final String[] groups = (m.groupCount() > 0 ? new String[m.groupCount()] : null);
			for(int i = 1; i <= m.groupCount(); i++) {
				groups[i - 1] = m.group(i);
			}
			onMatch(groups, event);
		}
	}
	
}
