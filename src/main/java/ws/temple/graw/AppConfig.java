package ws.temple.graw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.hsqldb.jdbc.JDBCDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {
	private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
	
	private final Properties props = new Properties();
	
	public enum Property {
		OAUTH_CLIENT_ID("oauth.client.id", null),
		OAUTH_CLIENT_SECRET("oauth.client.secret", null),
		OAUTH_REDIRECT_URI("oauth.redirect.uri", null),
		OAUTH_BOT_TOKEN("oauth.bot.token", null),
		JDBC_DRIVER("jdbc.driver", JDBCDriver.class.getName()),
		JDBC_URL("jdbc.url", "jdbc:hsqldb:file:data/configdb"),
		JDBC_USERNAME("jdbc.username", "sa"),
		JDBC_PASSWORD("jdbc.password", ""),
		SUPER_ADMIN_IDS("super.admin.ids", String[].class, new String[]{}, s -> s.split(",")),
		PASSWORD_KEYFILE("file.pass.key", File.class, new File("pass.key"), File::new),
		TOKEN_KEYFILE("file.token.key", File.class, new File("token.key"), File::new);
		
		/**
		 * Defines a configuration property.
		 * 
		 * @param id The identifier for the property as it should appear in the
		 *            configuration file
		 *           
		 * @param type The type the value should ultimately be cast
		 * 
		 * @param def The default value to use when the property is omitted
		 *             from the configuration file; if null, then the property
		 *             is understood to be required, and an exception will be
		 *             thrown if it is omitted
		 *            
		 * @param conv A Function accepting a String and returning the an
		 *              object of the parameter type
		 */
		<T> Property(String id, Class<T> type, T def, Function<String,T> conv) {
			this.id = id;
			this.type = type;
			this.defaultValue = def;
			this.transform = conv;
		}
		
		/**
		 * Defines a String configuration property.
		 * 
		 * @param id The identifier for the property as it should appear in the
		 *            configuration file
		 * 
		 * @param def The default value to use when the property is omitted
		 *             from the configuration file; if null, then the property
		 *             is understood to be required, and an exception will be
		 *             thrown if it is omitted
		 */
		Property(String id, String def) {
			this.id = id;
			this.type = String.class;
			this.defaultValue = def;
			this.transform = null;
		}
		
		private final String id;
		private final Class<?> type;
		private final Object defaultValue;
		private final Function<String,?> transform;
		
	}
	
	
	/**
	 * Constructs an instance for retrieving configuration properties from the
	 * specified file location.
	 * 
	 * @param configFile
	 */
	public AppConfig(File configFile) {
		if(configFile.exists()) {
			try(final FileInputStream fis = new FileInputStream(configFile);) {
				props.load(fis);
				for(Property param : Property.values()) {
					if(param.defaultValue == null && !props.containsKey(param.id))
						throw new RuntimeException("Missing required parameter " + param.id);
					else if(props.containsKey(param.id))
						LOG.info("Loaded parameter " + param.id);
					else
						LOG.info("Using default value for " + param.id);
				}
			}
			catch (IOException e) {
				LOG.error("Unable to load configuration file" + configFile.getPath(), e);
			}
		}
		else {
			try {
				createSkeletonFile(configFile);
				LOG.error("No configuration file was found; a skeleton file has been created at " + configFile.getPath());
			}
			catch (IOException e) {
				LOG.error("Unable to create skeleton configuration file", e);
			}
		}
	}
	
	
	/**
	 * Constructs an instance for retrieving configuration properties from the
	 * default file location.
	 * 
	 */
	public AppConfig() {
		this(new File(System.getProperty("ws.temple.graw.config", "graw.properties")));
	}
	
	
	/**
	 * Retrieves a String configuration property.
	 * 
	 * @param param
	 * @return
	 */
	public String get(Property param) {
		if(!String.class.isAssignableFrom(param.type))
			throw new ClassCastException("Parameter type mismatch; " + param.id + " refers to type " + param.type.getName());
		return props.getProperty(param.id);
	}
	
	
	/**
	 * Retrieves a typed configuration parameter.
	 * 
	 * @param param The parameter to retrieve
	 * @param type The type to cast to, which must match the property's type
	 * @return
	 */
	public <T> T get(Property param, Class<T> type) {
		if(!type.isAssignableFrom(param.type))
			throw new ClassCastException("Parameter type mismatch; " + param.id + " refers to type " + param.type.getName());
		final String prop = props.getProperty(param.id);
		if(prop == null)
			return type.cast(param.defaultValue);
		else if(param.transform == null)
			return type.cast(prop);
		else
			return type.cast(param.transform.apply(prop));
	}
	
	
	/**
	 * Generates a configuration file skeleton.
	 * 
	 * @throws IOException 
	 */
	public static void createSkeletonFile(File file) throws IOException {
		final StringBuilder sb = new StringBuilder();
		for(Property prop : Property.values()) {
			sb.append(String.format("%s=%s\n", prop.id, prop.defaultValue.toString()));
		}
		
		try(final FileOutputStream fos = new FileOutputStream(file);) {
			IOUtils.write(sb.toString(), fos);
		}
	}
	
}
