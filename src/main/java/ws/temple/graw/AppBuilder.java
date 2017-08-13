package ws.temple.graw;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.javanet.NetHttpTransport;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import ws.temple.graw.AppConfig.Property;
import ws.temple.graw.auth.AbstractAuthorizationFlowFilter;
import ws.temple.graw.auth.DatabaseCredentialStore;
import ws.temple.graw.auth.DiscordAuthFilter;
import ws.temple.graw.client.UserAPI;
import ws.temple.graw.config.BotConfigServlet;
import ws.temple.graw.config.CSRFFilter;
import ws.temple.graw.config.ConfigValidatorServlet;
import ws.temple.graw.crypt.AESCrypter;
import ws.temple.graw.crypt.Crypter;
import ws.temple.graw.crypt.CrypterException;
import ws.temple.graw.db.DatabaseVersioner;
import ws.temple.graw.db.QueryRunner;
import ws.temple.graw.db.dao.ConfigDAO;
import ws.temple.graw.db.dao.CredentialDAO;
import ws.temple.graw.listeners.DramaListener;
import ws.temple.graw.listeners.MaintenanceListener;
import ws.temple.graw.listeners.SVNListener;
import ws.temple.graw.svn.DefaultSVNMonitorFactory;
import ws.temple.graw.svn.SVNManager;

@WebListener
public class AppBuilder implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(AppBuilder.class);
	private static final String DISCORD_API_URL = "https://discordapp.com/api";

	private GuildManager guildMan;
	private SVNManager svnMan;
	private ScheduledThreadPoolExecutor exec;
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			final AppConfig config = new AppConfig();
			
			final JDA jda = buildDiscordAPI(config);
			final Crypter passCrypt = buildCrypter(config.get(Property.PASSWORD_KEYFILE, File.class));
			final Crypter tokenCrypt = buildCrypter(config.get(Property.TOKEN_KEYFILE, File.class));
			final DBI dbi = buildDataSource(config);
			
			// Wire up the managers
			guildMan = buildGuildManager(jda, config, new QueryRunner<>(dbi, ConfigDAO.class));
			svnMan = buildSVNManager(jda, new QueryRunner<>(dbi, ConfigDAO.class), passCrypt);
			guildMan.addConfigListener(svnMan);
			guildMan.addStatusListener(svnMan);
			jda.addEventListener(new SVNListener(svnMan, guildMan));
			jda.addEventListener(new MaintenanceListener(guildMan));
			guildMan.initialize();
			
			// Map servlets
			final ServletContext ctx = sce.getServletContext();
			final UserAPI userApi = new UserAPI(new NetHttpTransport(), DISCORD_API_URL);
			mapFilter(ctx, new CSRFFilter(), "/conf/*", "/validate");
			mapFilter(ctx, buildAuthFilter(config, dbi, tokenCrypt), "/conf/*", "/validate");
			mapServlet(ctx, new BotConfigServlet(guildMan, userApi), "/conf");
			mapServlet(ctx, new ConfigValidatorServlet(guildMan, svnMan, userApi, passCrypt), "/validate");
			
			LOG.info("Deployment complete");
		}
		catch (InterruptedException | LoginException | IllegalArgumentException | IOException | CrypterException e) {
			if(guildMan != null)
				guildMan.shutdown();
			throw new RuntimeException("Unable to initialize application", e);
		}
	}
	
	
	private Crypter buildCrypter(File keyFile) throws CrypterException, IOException {
		final byte[] key = IOUtils.toByteArray(new FileInputStream(keyFile));
		return new AESCrypter(key);
	}
	
	
	private DBI buildDataSource(AppConfig config) {
		final BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName(config.get(Property.JDBC_DRIVER));
		ds.setUrl(config.get(Property.JDBC_URL));
		ds.setUsername(config.get(Property.JDBC_USERNAME));
		ds.setPassword(config.get(Property.JDBC_PASSWORD));
		
		final DBI dbi = new DBI(ds);
		try(final Handle handle = dbi.open();) {
			new DatabaseVersioner(handle).execute();
		}
		
		return dbi;
	}
	
	
	private JDA buildDiscordAPI(AppConfig config) throws LoginException, IllegalArgumentException, InterruptedException {
		return new JDABuilder()
				.setBotToken(config.get(Property.OAUTH_BOT_TOKEN))
				.setBulkDeleteSplittingEnabled(false)
				.setAudioEnabled(false)
				.addListener(new DramaListener())
				.buildBlocking();
	}
	
	
	private GuildManager buildGuildManager(JDA jda, AppConfig config, QueryRunner<ConfigDAO> runner) throws LoginException, IllegalArgumentException, InterruptedException {
		final String clientId = config.get(Property.OAUTH_CLIENT_ID);
		final String[] sadmins = config.get(Property.SUPER_ADMIN_IDS, String[].class);
		return new GuildManager(jda, runner, clientId, sadmins);
	}
	
	
	private SVNManager buildSVNManager(JDA jda, QueryRunner<ConfigDAO> runner, Crypter crypt) {
		exec = new ScheduledThreadPoolExecutor(3);
		exec.setRemoveOnCancelPolicy(true);
		return new SVNManager(jda, crypt, new DefaultSVNMonitorFactory(runner, exec));
	}
	
	
	private AbstractAuthorizationFlowFilter buildAuthFilter(AppConfig config, DBI dbi, Crypter crypt) throws IOException {
		return new DiscordAuthFilter(
				config.get(Property.OAUTH_CLIENT_ID),
				config.get(Property.OAUTH_CLIENT_SECRET),
				config.get(Property.OAUTH_REDIRECT_URI),
				crypt,
				//MemoryDataStoreFactory.getDefaultInstance().getDataStore("graw_store"));
				new DatabaseCredentialStore("graw", new QueryRunner<>(dbi, CredentialDAO.class)));
	}
	
	
	private void mapServlet(ServletContext ctx, Servlet servlet, String... urlPatterns) {
		final ServletRegistration.Dynamic dyn = ctx.addServlet(servlet.getClass().getName(), servlet);
		for(String pattern : urlPatterns) {
			dyn.addMapping(pattern);
		}
	}
	
	
	private void mapFilter(ServletContext ctx, Filter filter, String... urlPatterns) {
		final FilterRegistration.Dynamic dyn = ctx.addFilter(filter.getClass().getName(), filter);
		dyn.addMappingForUrlPatterns(null, false, urlPatterns);
	}
	
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		guildMan.shutdown();
		svnMan.shutdown();
		exec.shutdown();
	}

}
