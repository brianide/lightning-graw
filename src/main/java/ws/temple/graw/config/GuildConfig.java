package ws.temple.graw.config;

import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class GuildConfig implements Serializable {
	private static final long serialVersionUID = -1190085806099764723L;
	
	private String repoUrl = "";
	private String username = "";
	private byte[] password = null;
	private int queryInterval = 180;
	private String logChannel = null;
	private String configRole = null;
	private boolean cmdResponse = true;
	private String dateFormat = "yyyy-MM-dd HH:mm:ss z";
	private String messageFormat = "**[{{auth}}]** *(r{{rnum}})* @ {{date}}```{{body}}```";
	
	public GuildConfig() {
	}
	
	public GuildConfig(GuildConfig orig) {
		this.repoUrl = orig.repoUrl;
		this.username = orig.username;
		this.password = orig.password;
		this.queryInterval = orig.queryInterval;
		this.logChannel = orig.logChannel;
		this.configRole = orig.configRole;
		this.cmdResponse = orig.cmdResponse;
		this.dateFormat = orig.dateFormat;
		this.messageFormat = orig.messageFormat;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public void setRepoUrl(String repoURL) {
		this.repoUrl = repoURL;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

	public int getQueryInterval() {
		return queryInterval;
	}

	public void setQueryInterval(int queryInterval) {
		this.queryInterval = queryInterval;
	}

	public String getLogChannel() {
		return logChannel;
	}

	public void setLogChannel(String logChannel) {
		this.logChannel = logChannel;
	}

	public String getMaintenanceRole() {
		return configRole;
	}

	public void setMaintenanceRole(String configRole) {
		this.configRole = configRole;
	}

	public boolean isResponsive() {
		return cmdResponse;
	}

	public void setResponsive(boolean cmdResponse) {
		this.cmdResponse = cmdResponse;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String getMessageFormat() {
		return messageFormat;
	}

	public void setMessageFormat(String messageFormat) {
		this.messageFormat = messageFormat;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "ServerConfig [repoUrl=" + repoUrl + ", username=" + username + ", password=" + Arrays.toString(password)
				+ ", queryInterval=" + queryInterval + ", logChannel=" + logChannel + ", configRole=" + configRole
				+ ", cmdResponse=" + cmdResponse + ", dateFormat=" + dateFormat + ", messageFormat=" + messageFormat + "]";
	}
	
}
