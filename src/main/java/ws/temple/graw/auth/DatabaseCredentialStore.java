package ws.temple.graw.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import ws.temple.graw.db.QueryRunner;
import ws.temple.graw.db.dao.CredentialDAO;

public class DatabaseCredentialStore implements DataStore<StoredCredential> {
	private final String id;
	private final QueryRunner<CredentialDAO> runner;
	
	public DatabaseCredentialStore(String id, QueryRunner<CredentialDAO> runner) {
		this.id = id;
		this.runner = runner;
	}
	
	@Override
	public DataStoreFactory getDataStoreFactory() {
		return null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int size() throws IOException {
		return runner.query(dao -> dao.getCredentialCount());
	}

	@Override
	public boolean isEmpty() throws IOException {
		return size() == 0;
	}

	@Override
	public boolean containsKey(String key) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(StoredCredential value) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<StoredCredential> values() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public StoredCredential get(String key) throws IOException {
		return runner.query(dao -> dao.getCredential(key));
	}

	@Override
	public DataStore<StoredCredential> set(String key, StoredCredential value) throws IOException {
		runner.executeTransaction(dao -> dao.putCredential(key, value));
		return this;
	}

	@Override
	public DataStore<StoredCredential> clear() throws IOException {
		runner.executeTransaction(dao -> dao.clearCredentials());
		return this;
	}

	@Override
	public DataStore<StoredCredential> delete(String key) throws IOException {
		runner.executeTransaction(dao -> dao.deleteCredential(key));
		return this;
	}
}
