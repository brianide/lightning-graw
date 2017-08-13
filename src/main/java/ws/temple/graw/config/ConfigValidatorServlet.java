package ws.temple.graw.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.google.api.client.auth.oauth2.Credential;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import ws.temple.graw.GuildManager;
import ws.temple.graw.ServletConstants;
import ws.temple.graw.client.UserAPI;
import ws.temple.graw.crypt.Crypter;
import ws.temple.graw.crypt.CrypterException;
import ws.temple.graw.svn.SVNManager;
import ws.temple.graw.svn.SVNStatus;

public class ConfigValidatorServlet extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigValidatorServlet.class);
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_REPO_URL = "svn_url";
	private static final String PARAM_USERNAME = "svn_un";
	private static final String PARAM_PASSWORD = "svn_pw";
	private static final String PARAM_QUERY_INTERVAL = "svn_int";
	private static final String PARAM_LOG_CHANNEL = "bot_chan";
	private static final String PARAM_MAINTENANCE_ROLE = "bot_role";
	private static final String PARAM_RESPONSIVE = "bot_cmd";
	private static final String PARAM_DATE_FORMAT = "fmt_date";
	private static final String PARAM_MESSAGE_FORMAT = "fmt_msg";
	private static final String PARAM_SERVER = "server";
	private static final String PARAM_IDENTITY = "identity";
	
	private static final int MINIMUM_QUERY_INTERVAL = 90;

	/** Handlebars instance for ensuring message format strings are parseable */
	private final Handlebars handlebars = new Handlebars();
	
	/** Mapper for creating the JSON response */
	private final ObjectMapper mapper = new ObjectMapper();
	
	private final GuildManager guildMan;
	private final SVNManager svnMan;
	private final UserAPI api;
	private final Crypter crypt;
	
	public ConfigValidatorServlet(GuildManager guildMan, SVNManager svnMan, UserAPI api, Crypter crypt) {
		this.guildMan = guildMan;
		this.svnMan = svnMan;
		this.api = api;
		this.crypt = crypt;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	/**
	 * Serialize the response to a JSON object and send it.
	 * 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().write(mapper.writeValueAsString(buildResponse(request)));
	}
	
	/**
	 * Validate form input and construct the response.
	 * 
	 * @param req
	 * @param resp
	 * @return
	 */
	public Map<String,Object> buildResponse(HttpServletRequest req) {
		
		// Verify that the server ID corresponds to a real server that Graw is
		// running on
		final String serverId = ObjectUtils.firstNonNull(req.getParameter(PARAM_SERVER), "0");
		final Guild server = guildMan.getDiscord().getGuildById(serverId);
		final GuildConfig config = new GuildConfig(guildMan.getConfig(serverId, true));
		final Map<String,String[]> params = req.getParameterMap();
		boolean valid = (server != null);
		
		// Verify CSRF token
		if(valid) {
			valid = checkCSRFToken(params, req);
		}
		
		// Verify that the user is authorized to modify the server configuration
		if(valid) {
			final Credential cred = (Credential) req.getAttribute(ServletConstants.REQ_ATTR_CREDENTIAL);
			try {
				valid = guildMan.checkManagementPermission(serverId, api.getUser(cred).getId());
			}
			catch (IOException e) {
				LOG.error("Exception while retrieving user information", e);
				valid = false;
			}
		}
		
		// Verify the individual fields, or skip this phase if we've already
		// run into a problem
		final List<String> errors = new ArrayList<>();
		if(valid) {
			valid = checkFormFields(params, server, config, errors);
		}
		
		// If there were input format errors, send an error result
		final Map<String,Object> response = new HashMap<>();
		if(!valid || !errors.isEmpty()) {
			response.put("status", "error");
			response.put("message", "<strong>Unable to apply changes.</strong> Please check the validity of the highlighted fields and try again.");
			response.put("errors", errors);
		}
		
		// Save the config and verify SVN
		else {
			guildMan.updateConfig(serverId, config);
			final SVNStatus status = svnMan.getMonitor(serverId).getStatus();
			if(status == SVNStatus.NORMAL) {
				response.put("status", "success");
				response.put("message", "<strong>Configuration changes applied successfully.</strong>");
			}
			else {
				response.put("status", "warning");
				
				if(status == SVNStatus.BAD_CREDENTIALS)
					response.put("message", "The changes were applied successfully, but the supplied credentials were rejected by the repository.");
				else if(status == SVNStatus.NO_CONNECTION)
					response.put("message", "The changes were applied successfully, but no connection to the repository could be established at this time.");
			}
		}
		
		return response;
	}
	
	private boolean checkCSRFToken(Map<String,String[]> params, HttpServletRequest req) {
		if(params.containsKey(PARAM_IDENTITY)) {
			final String identity = params.get(PARAM_IDENTITY)[0];
			final Optional<String> tok = Stream.of(req.getCookies())
					.filter(c -> c.getName().equals(ServletConstants.COOKIE_CSRF_TOKEN))
					.map(Cookie::getValue)
					.findFirst();
			
			return (identity != null && tok.isPresent() && tok.get().equals(identity));
		}
		else
			return false;
	}
	
	/**
	 * Validate the POSTed form fields, report errors, and populate the new
	 * configuration object with the valid entries.
	 * 
	 * @param params
	 * @param server
	 * @param config
	 * @param errors
	 * @return
	 */
	private boolean checkFormFields(Map<String,String[]> params, Guild server, GuildConfig config, List<String> errors) {
		boolean valid = true;
		for (Entry<String, String[]> entry : params.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue()[0];
			LOG.debug("Checking POST parameter {} => {}", key, value);

			switch (key) {
			case PARAM_REPO_URL:
				String protocol = null;
				try {
					protocol = new URL(value).getProtocol();
				}
				catch(MalformedURLException e) {}
				
				if(protocol == null || !protocol.equals("https")) {
					errors.add(PARAM_REPO_URL);
					valid = false;
					LOG.debug("Rejected URL {}", value);
				}
				else {
					config.setRepoUrl(value);
					LOG.debug("Accepted URL {}", value);
				}
				break;

			case PARAM_USERNAME:
				if(!value.isEmpty())
					config.setUsername(value);
				else {
					errors.add(PARAM_USERNAME);
					valid = false;
				}
				break;

			case PARAM_PASSWORD:
				if(value.isEmpty() && config.getPassword() == null) {
					errors.add(PARAM_PASSWORD);
					valid = false;
				}
				else if(!value.isEmpty()) {
					try {
						config.setPassword(crypt.encrypt(value.toCharArray(), "UTF-8"));
					}
					catch (CrypterException e) {
						LOG.error("Error while encrypting password", e);
						errors.add(PARAM_PASSWORD);
						valid = false;
					}
				}
				
				break;

			case PARAM_QUERY_INTERVAL:
				final int queryInt = parseNonNegativeInteger(value);
				if(queryInt >= MINIMUM_QUERY_INTERVAL)
					config.setQueryInterval(queryInt);
				else {
					errors.add(PARAM_QUERY_INTERVAL);
					valid = false;
				}
				break;

			case PARAM_LOG_CHANNEL:
				final String chan = server.getTextChannels()
						.stream()
						.map(TextChannel::getId)
						.filter(id -> id.equals(value))
						.findFirst()
						.orElse(null);
				if(chan != null || parseNonNegativeInteger(value) == 0) {
					config.setLogChannel(chan);
				}
				else {
					errors.add(PARAM_LOG_CHANNEL);
					valid = false;
				}
				break;
				
			case PARAM_MAINTENANCE_ROLE:
				final String role = server.getRoles()
						.stream()
						.map(Role::getId)
						.filter(id -> id.equals(value))
						.findFirst()
						.orElse(null);
				if(role != null || parseNonNegativeInteger(value) == 0) {
					config.setMaintenanceRole(role);
				}
				else {
					errors.add(PARAM_LOG_CHANNEL);
					valid = false;
				}
				break;

			case PARAM_DATE_FORMAT:
				if(validateDateFormat(value))
					config.setDateFormat(value);
				else {
					errors.add(PARAM_DATE_FORMAT);
					valid = false;
				}
				break;

			case PARAM_MESSAGE_FORMAT:
				if(validateMessageFormat(value))
					config.setMessageFormat(value);
				else {
					errors.add(PARAM_MESSAGE_FORMAT);
					valid = false;
				}
				break;

			}
		}
		
		// Boolean parameters have to be checked differently
		config.setResponsive(params.containsKey(PARAM_RESPONSIVE));
		
		return valid;
	}
	
	/**
	 * Returns the integer value represented by the passed String, or -1 if
	 * the String does not represent a non-negative integer value.
	 * 
	 * @param input
	 * @return
	 */
	private int parseNonNegativeInteger(String input) {
		int result = -1;
		try {
			result = Integer.parseInt(input);
		}
		catch (NumberFormatException e) {}
		return result;
	}
	
	/**
	 * Validate a date format string with shitty exception flow control.
	 * 
	 * @param fmt
	 * @return
	 */
	private boolean validateDateFormat(String fmt) {
		boolean success = true;
		try {
			new SimpleDateFormat(fmt);
		}
		catch (IllegalArgumentException e) {
			success = false;
		}
		return success;
	}
	
	/**
	 * Validate a Handlebars format string with shitty exception flow control.
	 * 
	 * @param fmt
	 * @return
	 */
	private boolean validateMessageFormat(String fmt) {
		boolean success = true;
		try {
			handlebars.compileInline(fmt);
		}
		catch (IOException e) {
			success = false;
		}
		return success;
	}
}
