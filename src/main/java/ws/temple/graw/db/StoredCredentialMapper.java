package ws.temple.graw.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.api.client.auth.oauth2.StoredCredential;

public class StoredCredentialMapper implements ResultSetMapper<StoredCredential> {

	@Override
	public StoredCredential map(int index, ResultSet r, StatementContext ctx) throws SQLException {
		final ResultSetMetaData meta = r.getMetaData();
		for(int i = 1; i <= meta.getColumnCount(); i++) {
			if(meta.getColumnType(i) == Types.OTHER) {
				return (StoredCredential) r.getObject(i);
			}
		}
		return null;
	}
	
}
