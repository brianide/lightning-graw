package ws.temple.graw;

import java.util.Optional;

import ws.temple.graw.config.GuildConfig;

public interface GuildConfigListener {
	
	void onConfigChange(String id, Optional<GuildConfig> oldConfig, GuildConfig newConfig);

}
