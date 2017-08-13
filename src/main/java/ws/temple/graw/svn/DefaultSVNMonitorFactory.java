package ws.temple.graw.svn;

import java.util.concurrent.ScheduledExecutorService;

import ws.temple.graw.db.QueryRunner;
import ws.temple.graw.db.dao.ConfigDAO;

public class DefaultSVNMonitorFactory implements SVNMonitorFactory {

	private final QueryRunner<ConfigDAO> runner;
	private final ScheduledExecutorService exec;

	public DefaultSVNMonitorFactory(QueryRunner<ConfigDAO> runner, ScheduledExecutorService exec) {
		this.runner = runner;
		this.exec = exec;
	}
	
	@Override
	public SVNMonitor createMonitor(String id) {
		return new SVNMonitor(id, runner, exec);
	}
	
}
