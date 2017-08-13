package ws.temple.graw.listeners;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Message.Attachment;
import net.dv8tion.jda.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AccountManager;
import net.dv8tion.jda.utils.AvatarUtil;
import ws.temple.graw.GuildManager;

public class MaintenanceListener extends ListenerAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(MaintenanceListener.class);

	private GuildManager guildMan;
	
	public MaintenanceListener(GuildManager guildMan) {
		this.guildMan = guildMan;
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		final Message msg = event.getMessage();
		if(msg.getContent().equals("!setAvatar") && guildMan.checkManagementPermission(null, event.getAuthor().getId())) {
			final List<Attachment> attachments = msg.getAttachments();
			if(attachments.size() > 0 && attachments.get(0).isImage()) {
				try {
					// There's certainly a better way of doing this, but using
					// the URL directly keeps coming back with 403 errors, so
					// fuck it.
					final Attachment image = attachments.get(0);
					final File temp = File.createTempFile("graw-", "-" + image.getFileName());
					temp.delete();
					attachments.get(0).download(temp);

					final AccountManager accountMan = guildMan.getDiscord().getAccountManager();
					accountMan.setAvatar(AvatarUtil.getAvatar(temp));
					accountMan.update();
					temp.delete();
					
					event.getChannel().sendMessageAsync("Avatar updated", null);
				}
				catch(IOException e) {
					LOG.error("Exception while updating avatar", e);
					event.getChannel().sendMessageAsync("Unable to update avatar; " + e.getMessage(), null);
				}
			}
			else {
				event.getChannel().sendMessageAsync("Please attach an image to the !setAvatar command", null);
			}
		}
	}

}
