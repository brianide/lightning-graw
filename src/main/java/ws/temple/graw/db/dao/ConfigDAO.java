package ws.temple.graw.db.dao;

import java.io.Closeable;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.tweak.BeanMapperFactory;

import ws.temple.graw.config.GuildConfig;

@RegisterMapperFactory(BeanMapperFactory.class)
public abstract class ConfigDAO implements Closeable {
	
	@SqlUpdate("UPDATE configs SET repo_url=:c.repoUrl, username=:c.username, password=:c.password, interval=:c.queryInterval, responsive=:c.responsive, date_fmt=:c.dateFormat, message_fmt=:c.messageFormat, channel_id=:c.logChannel, maintainers_id=:c.maintenanceRole WHERE guild_id = :gid;")
	public abstract int updateConfig(@Bind("gid") String gid, @BindBean("c") GuildConfig config);
	
	@SqlUpdate("INSERT INTO configs (guild_id, repo_url, username, password, interval, responsive, date_fmt, message_fmt, channel_id, maintainers_id) VALUES (:gid, :c.repoUrl, :c.username, :c.password, :c.queryInterval, :c.responsive, :c.dateFormat, :c.messageFormat, :c.logChannel, :c.maintenanceRole);")
	public abstract void insertConfig(@Bind("gid") String gid, @BindBean("c") GuildConfig config);
	
	public void putConfig(String gid, GuildConfig config) {
		if(updateConfig(gid, config) == 0)
			insertConfig(gid, config);
	}
	
	@SqlUpdate("UPDATE configs SET last_rev=:rev WHERE guild_id=:gid;")
	public abstract int updateLatestRevision(@Bind("gid") String guildId, @Bind("rev") long revision);
	
	@SqlQuery("SELECT last_rev FROM configs WHERE guild_id=:gid")
	public abstract long getLatestRevision(@Bind("gid") String gid);
	
	@SqlQuery("SELECT repo_url AS repoUrl, username, password, interval AS queryInterval, responsive, date_fmt AS dateformat, message_fmt AS messageFormat, channel_id AS logChannel, maintainers_id AS maintenanceRole FROM configs WHERE guild_id = :gid")
	public abstract GuildConfig getConfig(@Bind("gid") String gid);
	
}
