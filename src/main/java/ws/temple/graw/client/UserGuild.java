package ws.temple.graw.client;

public class UserGuild {
	
	private String id;
	private String name;
	private String icon;
	private boolean owner;
	private int permissions;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getIcon() {
		return icon;
	}
	
	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	public boolean isOwner() {
		return owner;
	}
	
	public void setOwner(boolean owner) {
		this.owner = owner;
	}
	
	public int getPermissions() {
		return permissions;
	}
	
	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}
	
}
