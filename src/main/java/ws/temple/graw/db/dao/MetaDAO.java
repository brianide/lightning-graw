package ws.temple.graw.db.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public abstract class MetaDAO {

	public void setVersion(int id) {
		putValue("version", Integer.toString(id));
	}
	
	public int getVersion() {
		final String value = getValue("version");
		return (value == null ? 0 : Integer.parseInt(value));
	}
	
	@SqlQuery("SELECT meta_value FROM meta_info WHERE meta_key=:key;")
	public abstract String getValue(@Bind("key") String key);
	
	@SqlUpdate("UPDATE meta_info SET meta_value=:value WHERE meta_key=:key")
	public abstract int updateValue(@Bind("key") String key, @Bind("value") String value);
	
	@SqlUpdate("INSERT INTO meta_info (meta_key,meta_value) VALUES (:key,:value);")
	public abstract void insertValue(@Bind("key") String key, @Bind("value") String value);
	
	public void putValue(String key, String value) {
		if(updateValue(key, value) == 0)
			insertValue(key, value);
	}
	
}
