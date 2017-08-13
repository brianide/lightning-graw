package ws.temple.graw.auth;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;

public abstract class AbstractAuthorizationFlowFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractAuthorizationFlowFilter.class);

	private final String tokenServerUrl;
	private final String authServerUrl;
	private final List<String> scopes;
	
	private final String clientId;
	private final String clientSecret;
	private final String redirect;
	private final DataStore<StoredCredential> store;
	
	private AuthorizationCodeFlow flow;
	
	/**
	 * Default constructor.
	 * 
	 * @param tokenServerUrl
	 * @param authServerUrl
	 * @param scopes
	 * @param clientId
	 * @param clientSecret
	 * @param store
	 */
	public AbstractAuthorizationFlowFilter(String tokenServerUrl, String authServerUrl, List<String> scopes, String clientId, String clientSecret, String redirect, DataStore<StoredCredential> store) {
		this.tokenServerUrl = tokenServerUrl;
		this.authServerUrl = authServerUrl;
		this.scopes = scopes;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirect = redirect;
		this.store = store;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
				new NetHttpTransport(),
				JacksonFactory.getDefaultInstance(),
				new GenericUrl(tokenServerUrl),
				new ClientParametersAuthentication(clientId, clientSecret),
				clientId,
				authServerUrl)
				.setCredentialDataStore(store)
				.setScopes(scopes)
				.build();
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if(request instanceof HttpServletRequest) {
			final HttpServletRequest req = (HttpServletRequest) request;
			final HttpServletResponse resp = (HttpServletResponse) response;
			
			Credential cred = flow.loadCredential(establishIdentifier(req, resp));
			try {
				if(cred == null) {
					final String authCode = (String) req.getParameter("code");
					final String stateParam = (String) req.getParameter("state");
					if(authCode == null) {
						final String stateTok = generateStateToken(req, resp);
						LOG.debug("Generated state token: {}", stateTok);
						if(stateTok != null) {
							final String authReq = flow.newAuthorizationUrl()
									.setRedirectUri(redirect)
									.setState(stateTok)
									.build();
							resp.sendRedirect(authReq);
							return;
						}
					}
					else if(validateStateToken(stateParam, req, resp)) {
						final TokenResponse tokenResp = flow.newTokenRequest(authCode)
								.setRedirectUri(redirect)
								.execute();
						cred = flow.createAndStoreCredential(tokenResp, establishIdentifier(req, resp));
					}
					else {
						LOG.debug("State token mismatch; {} != {}", stateParam, generateStateToken(req, resp));
					}
				}
			}
			catch(IOException e) {
				LOG.warn("Exceptional response to token request", e);
			}
			
			if(cred != null) {
				LOG.debug("Validated user token {}", cred.getAccessToken());
				onSuccess(req, resp, cred);
				chain.doFilter(request, response);
			}
			else {
				LOG.debug("Validation failure");
				onFailure(req, resp);
			}
		}
		
		// Don't process non-HTTP requests
		else {
			chain.doFilter(request, response);
		}
	}
	
	/**
	 * Retrieve the user's identifier, creating one if necessary.
	 * 
	 * @param req
	 * @param resp
	 * @return
	 */
	protected String establishIdentifier(HttpServletRequest req, HttpServletResponse resp) {
		String id = getIdentifier(req);
		if(id == null || id.isEmpty()) {
			id = createIdentifier(req, resp);
		}
		else {
		}
		if(id == null || id.isEmpty())
			throw new IllegalStateException("New identifiers may not be null or blank");
		
		return id;
	}
	
	/**
	 * Returns a unique identifier for the session user, or an empty string if
	 * none has been established.
	 * 
	 * @param req
	 * @return
	 */
	protected abstract String getIdentifier(HttpServletRequest req);
	
	/**
	 * Creates a new unique identifier for the session user.
	 * 
	 * @param req
	 * @param resp
	 * @return
	 */
	protected abstract String createIdentifier(HttpServletRequest req, HttpServletResponse resp);
	
	/**
	 * Generates the state value.
	 * 
	 * @param req
	 * @param resp
	 * @return
	 */
	protected abstract String generateStateToken(HttpServletRequest req, HttpServletResponse resp);
	
	/**
	 * Validates a state token against the expected value.
	 * 
	 * @param token
	 * @param req
	 * @param resp
	 * @return
	 */
	protected abstract boolean validateStateToken(String token, HttpServletRequest req, HttpServletResponse resp);
	
	/**
	 * Handler called upon successful authorization.
	 * 
	 * @param req
	 * @param resp
	 */
	protected abstract void onSuccess(HttpServletRequest req, HttpServletResponse resp, Credential cred);

	/**
	 * Handler called upon authorization failure.
	 * 
	 * @param req
	 * @param resp
	 */
	protected abstract void onFailure(HttpServletRequest req, HttpServletResponse resp);
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {}

}
