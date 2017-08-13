package ws.temple.graw.db.dao;

import java.io.Closeable;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import com.google.api.client.auth.oauth2.StoredCredential;

import ws.temple.graw.db.StoredCredentialMapper;

@RegisterMapper(StoredCredentialMapper.class)
public abstract class CredentialDAO implements Closeable {
	
	/* CRUD operations */
	
	@SqlQuery("SELECT stored_cred FROM discord_creds WHERE user_id=:uid;")
	public abstract StoredCredential getCredential(@Bind("uid") String userId);
	
	@SqlUpdate("INSERT INTO discord_creds (user_id,stored_cred) VALUES (:uid, :cred);")
	public abstract void insertCredential(@Bind("uid") String userId, @Bind("cred") StoredCredential cred);

	@SqlUpdate("UPDATE discord_creds SET stored_cred=:cred WHERE user_id=:uid;")
	public abstract int updateCredential(@Bind("uid") String userId, @Bind("cred") StoredCredential cred);
	
	@SqlUpdate("DELETE FROM discord_creds WHERE user_id=:uid;")
	public abstract void deleteCredential(@Bind("uid") String userId);

	public void putCredential(String userId, StoredCredential cred) {
		if(updateCredential(userId, cred) == 0)
			insertCredential(userId, cred);
	}
	
	/* Psuedo-list operation for satisfying the DataStore interface */
	
	@SqlQuery("SELECT stored_cred FROM discord_creds;")
	public abstract List<StoredCredential> getAllCredentials();
	
	@SqlQuery("SELECT COUNT(*) FROM discord_creds")
	public abstract int getCredentialCount();

	@SqlUpdate("DELETE FROM discord_creds;")
	public abstract void clearCredentials();
	
}
