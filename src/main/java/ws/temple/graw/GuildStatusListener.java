package ws.temple.graw;

import java.util.Optional;

import ws.temple.graw.config.GuildConfig;

public interface GuildStatusListener {
	
	void onGuildActivated(String id, Optional<GuildConfig> config);
	
	void onGuildDeactivated(String id);

}
