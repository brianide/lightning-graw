package ws.temple.graw.listeners;

import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class DramaListener extends ListenerAdapter {
	
	private static final String DRAMA = "%U% the works on project still continue so don't make drama on discord telling everyone that the project is dead.";
	private static final String GOOD_SHIT = "%U% :ok_hand::eyes::ok_hand::eyes::ok_hand::eyes::ok_hand::eyes::ok_hand::eyes: good shit go0ԁ sHit :ok_hand: thats :white_check_mark: some good :ok_hand::ok_hand: shit right :ok_hand::ok_hand: there :ok_hand::ok_hand::ok_hand: right :white_check_mark: there :white_check_mark::white_check_mark: if i do ƽaү so my self :100: i say so :100: thats what im talking about right there right there (chorus: ʳᶦᵍʰᵗ ᵗʰᵉʳᵉ) mMMMMᎷМ :100::ok_hand::ok_hand::ok_hand: НO0ОଠOOOOOОଠଠOoooᵒᵒᵒᵒᵒᵒᵒᵒᵒ :ok_hand: :ok_hand::ok_hand: :ok_hand::100::ok_hand::eyes::eyes::eyes::ok_hand::ok_hand: Good shit";

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		final String content = event.getMessage().getContent();
		if(!event.getAuthor().isBot()) {
			if(content.contains("cancelled")) {
				final Message msg = new MessageBuilder().appendFormat(DRAMA, event.getAuthor()).build();
				event.getChannel().sendMessageAsync(msg, null);
			}
			if(content.contains("good shit")) {
				final Message msg = new MessageBuilder().appendFormat(GOOD_SHIT, event.getAuthor()).build();
				event.getChannel().sendMessageAsync(msg, null);
			}
		}
	}

}
