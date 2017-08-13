package ws.temple.graw;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
	
	private Utils(){}
	
	/**
	 * Traverses the given list of listeners, pruning expired WeakReferences
	 * and applying the passed Consumer function to the live ones.
	 * 
	 * @param listeners
	 * @param func
	 */
	public static <T> void fireListeners(Collection<WeakReference<T>> listeners, Consumer<T> func) {
		final Iterator<WeakReference<T>> iter = listeners.iterator();
		while(iter.hasNext()) {
			final T listener = iter.next().get();
			if(listener != null)
				func.accept(listener);
			else
				iter.remove();
		}
	}
	
	/**
	 * Gently cancel a Future. If processing has already begun, this method
	 * will block until it completes; otherwise, the Future is cancelled
	 * and this method returns immediately.
	 * 
	 * @param future
	 */
	public static void blockCancel(Future<?> future) {
		try {
			if(!future.cancel(false))
					future.get();
		}
		catch (CancellationException | InterruptedException | ExecutionException e) {
			LOG.debug("Expected exception while cancelling task", e);
		}
	}
	
	/**
	 * Creates a timed-out cookie suitable for expiring the named cookie on the
	 * client.
	 * 
	 * @param name
	 * @return
	 */
	public static Cookie createKillCookie(String name) {
		final Cookie cookie = new Cookie(name, null);
		cookie.setMaxAge(0);
		return cookie;
	}

}
