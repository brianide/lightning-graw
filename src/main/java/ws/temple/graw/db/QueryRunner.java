package ws.temple.graw.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryRunner<T extends Closeable> {
	private static final Logger LOG = LoggerFactory.getLogger(QueryRunner.class);
	
	private final DBI dbi;
	private final Class<T> type;
	
	public QueryRunner(DBI dbi, Class<T> type) {
		this.dbi = dbi;
		this.type = type;
	}

	/**
	 * Applies the passed function to a Handle and jDBI object derived from
	 * the passed DBI instance.
	 * 
	 * @param query
	 */
	public void execute(BiConsumer<Handle,T> query) {
		try(	final Handle handle = dbi.open();
				final T dao = handle.attach(type);) {
			query.accept(handle, dao);
		}
		catch (IOException e) {
			LOG.error("Exception while closing handle", e);
		}
	}
	
	/**
	 * Applies the passed function to a jDBI object derived from the passed DBI
	 * instance.
	 * 
	 * @param query
	 */
	public void execute(Consumer<T> query) {
		execute((hand, dao) -> query.accept(dao));
	}
	
	/**
	 * Applies the passed function to a jDBI object derived from the passed DBI
	 * instance. The function is executed inside of a transaction at the
	 * environment's default isolation level.
	 * 
	 * @param query
	 */
	public void executeTransaction(Consumer<T> query) {
		execute((hand, dao) -> {
			hand.begin();
			query.accept(dao);
			hand.commit();
		});
	}
	
	/**
	 * Applies the passed function to a Handle and jDBI object derived from
	 * the passed DBI instance, and returns the result.
	 * 
	 * @param query
	 */
	public <V> V query(BiFunction<Handle,T,V> query) {
		try(	final Handle handle = dbi.open();
				final T dao = handle.attach(type);) {
			return query.apply(handle, dao);
		}
		catch (IOException e) {
			LOG.error("Exception while closing handle", e);
		}
		return null;
	}
	
	/**
	 * Applies the passed function to a jDBI object derived from the passed DBI
	 * instance, and returns the result.
	 * 
	 * @param query
	 */
	public <V> V query(Function<T,V> query) {
		return query((hand, dao) -> query.apply(dao));
	}
	
	/**
	 * Applies the passed function to a jDBI object derived from the passed DBI
	 * instance, and returns the result. The function is executed inside of a
	 * transaction at the environment's default isolation level.
	 * 
	 * @param query
	 */
	public <V> V doTransaction(Function<T,V> query) {
		return query((hand, dao) -> {
			hand.begin();
			final V result = query.apply(dao);
			hand.commit();
			return result;
		});
	}
	
}
