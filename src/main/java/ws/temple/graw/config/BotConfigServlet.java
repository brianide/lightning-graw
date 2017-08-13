package ws.temple.graw.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;

import net.dv8tion.jda.entities.Guild;
import ws.temple.graw.GuildManager;
import ws.temple.graw.ServletConstants;
import ws.temple.graw.Utils;
import ws.temple.graw.client.User;
import ws.temple.graw.client.UserAPI;
import ws.temple.graw.client.UserGuild;

public class BotConfigServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private final GuildManager guildMan;
	private final UserAPI userApi;
	
	
	public BotConfigServlet(GuildManager bot, UserAPI userApi) {
		this.guildMan = bot;
		this.userApi = userApi;
	}

	/**
	 * Directs an incoming request to the appropriate handler.
	 * 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(request.getParameter("logout") != null) {
			request.getSession().invalidate();
			response.addCookie(Utils.createKillCookie(ServletConstants.COOKIE_IDENTIFIER));
			response.sendRedirect(request.getContextPath());
			return;
		}
		
		final Credential cred = (Credential) request.getAttribute(ServletConstants.REQ_ATTR_CREDENTIAL);
		if(request.getParameter("server") != null) {
			handleConfigPage(request, response, cred, null);
		}
		else {
			handleSelectPage(request, response, cred);
		}
	}
	
	/**
	 * Handle the initial display of the configuration page, as well as
	 * redisplay with feedback after a POST submission.
	 * 
	 * @param req
	 * @param resp
	 * @param cred
	 * @param errors Any errors that occurred while validating the POST submission, or null
	 * @throws ServletException
	 * @throws IOException
	 */
	public void handleConfigPage(HttpServletRequest req, HttpServletResponse resp, Credential cred, Set<String> errors) throws ServletException, IOException {
		final User user = userApi.getUser(cred);
		final String serverID = req.getParameter("server");
		final Guild server = guildMan.getDiscord().getGuildById(serverID);
		final GuildConfig conf = guildMan.getConfig(serverID, true);
		
		// The user probably fucked something up, so we bail
		if(server == null) {
			handleSelectPage(req, resp, cred);
			return;
		}
		
		final Optional<String> csrfTok = Stream.of(req.getCookies())
				.filter(c -> c.getName().equals(ServletConstants.COOKIE_CSRF_TOKEN))
				.map(Cookie::getValue)
				.findFirst();
		
		final BotConfigModel model = new BotConfigModel(user, server, conf)
				.setChannels(server.getTextChannels())
				.setRoles(server.getRoles())
				.setCsrfToken(csrfTok.orElse(""));
		
		req.setAttribute("model", model);
		req.getRequestDispatcher("bot-config.view.jsp").forward(req, resp);
	}

	/**
	 * 
	 * @param req
	 * @param resp
	 * @param cred
	 * @throws ServletException
	 * @throws IOException
	 */
	public void handleSelectPage(HttpServletRequest req, HttpServletResponse resp, Credential cred) throws ServletException, IOException {
		final User user = userApi.getUser(cred);
		final List<UserGuild> servers = userApi.getUserGuilds(cred)
				.stream()
				.filter(g -> guildMan.isConnected(g.getId()) && guildMan.checkManagementPermission(g.getId(), user.getId()))
				.collect(Collectors.toList());
		
		final GuildSelectModel model = new GuildSelectModel(user, guildMan.getClientId())
				.setServers(servers);
		
		
		req.setAttribute("model", model);
		req.getRequestDispatcher("server-select.view.jsp").forward(req, resp);
	}

}
