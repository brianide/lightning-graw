package ws.temple.graw.db;

import org.skife.jdbi.v2.Handle;

import ws.temple.graw.db.dao.MetaDAO;

public class DatabaseVersioner {
	
	private static final int CURRENT_VERSION = 1;
	private final Handle handle;
	
	public DatabaseVersioner(Handle handle) {
		this.handle = handle;
	}
	
	public void execute() {
		handle.begin();
		handle.createScript("sql/bootstrap.sql").execute();
		final MetaDAO dao = handle.attach(MetaDAO.class);
		final int dbVersion = dao.getVersion();
		
		for(int i = dbVersion; i < CURRENT_VERSION; i++) {
			handle.createScript(String.format("sql/%04d.sql", i)).execute();
		}

		dao.setVersion(CURRENT_VERSION);

		handle.commit();
	}

}
