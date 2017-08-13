package ws.temple.graw.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;

import ws.temple.graw.ServletConstants;
import ws.temple.graw.crypt.Crypter;
import ws.temple.graw.crypt.CrypterException;

public class DiscordAuthFilter extends AbstractAuthorizationFlowFilter {
	private static final Logger LOG = LoggerFactory.getLogger(DiscordAuthFilter.class);
	private static final Base64.Encoder ENCODER = Base64.getEncoder();
	private static final Base64.Decoder DECODER = Base64.getDecoder();
	
	private static final String TOKEN_SERVER_URL = "https://discordapp.com/api/oauth2/token";
	private static final String AUTH_SERVER_URL = "https://discordapp.com/api/oauth2/authorize";
	private static final List<String> SCOPES = Arrays.asList("identify", "guilds");
	
	private final Crypter crypt;
	
	/**
	 * Default constructor.
	 * 
	 * @param clientId
	 * @param clientSecret
	 * @param redirect
	 * @param store
	 */
	public DiscordAuthFilter(String clientId, String clientSecret, String redirect, Crypter crypt, DataStore<StoredCredential> store) {
		super(TOKEN_SERVER_URL, AUTH_SERVER_URL, SCOPES, clientId, clientSecret, redirect, store);
		this.crypt = crypt;
	}

	@Override
	protected String getIdentifier(HttpServletRequest req) {
		return Stream.of(req.getCookies())
				.filter(c -> c.getName().equals(ServletConstants.COOKIE_IDENTIFIER))
				.map(Cookie::getValue)
				.findFirst()
				.orElse("");
	}

	@Override
	protected String createIdentifier(HttpServletRequest req, HttpServletResponse resp) {
		final String id = UUID.randomUUID().toString();
		final Cookie cookie = new Cookie(ServletConstants.COOKIE_IDENTIFIER, id);
		cookie.setHttpOnly(true);
		cookie.setMaxAge(60 * 60 * 24 * 15);
		resp.addCookie(cookie);
		return id;
	}
	
	@Override
	protected String generateStateToken(HttpServletRequest req, HttpServletResponse resp) {
		try {
			final byte[] enc = crypt.encrypt(establishIdentifier(req, resp).toCharArray(), "UTF-8");
			return ENCODER.encodeToString(enc);
		}
		catch (CrypterException e) {
			LOG.error("Exception while encrypting state token", e);
		}
		return null;
	}
	
	@Override
	protected boolean validateStateToken(String token, HttpServletRequest req, HttpServletResponse resp) {
		try {
			final String dec = new String(crypt.decrypt(DECODER.decode(token), "UTF-8"));
			LOG.debug("Decrypted identifier: {}", dec);
			LOG.debug("Expected identifier: {}", getIdentifier(req));
			return getIdentifier(req).equals(dec);
		}
		catch (CrypterException e) {
			LOG.error("Exception while decrypting token", e);
		}
		return false;
	}

	@Override
	protected void onSuccess(HttpServletRequest req, HttpServletResponse resp, Credential cred) {
		req.setAttribute(ServletConstants.REQ_ATTR_CREDENTIAL, cred);
	}

	@Override
	protected void onFailure(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
		catch (IOException e) {
			LOG.error("Exception while sending error redirect", e);
		}
	}

}
