package ws.temple.graw.config;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ws.temple.graw.ServletConstants;

public class CSRFFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if(request instanceof HttpServletRequest) {
			final boolean hasToken = Stream.of(((HttpServletRequest) request).getCookies())
					.anyMatch(c -> c.getName().equals(ServletConstants.COOKIE_CSRF_TOKEN));
			
			if(!hasToken) {
				((HttpServletResponse) response).addCookie(new Cookie(ServletConstants.COOKIE_CSRF_TOKEN, UUID.randomUUID().toString()));
			}
		}
		chain.doFilter(request, response);
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void destroy() {}

}
