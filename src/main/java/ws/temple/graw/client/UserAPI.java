package ws.temple.graw.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;

public class UserAPI {

	private final HttpTransport transport;
	private final ObjectMapper mapper;
	
	private final String userQueryUrl;
	private final String userGuildsQueryUrl;
	
	public UserAPI(HttpTransport transport, String baseApiUrl) {
		this.transport = transport;

		userQueryUrl = baseApiUrl + "/users/@me";
		userGuildsQueryUrl = baseApiUrl + "/users/@me/guilds";
		mapper = new ObjectMapper();
	}
	
	public User getUser(Credential cred) throws IOException {
		return executeQuery(cred, userQueryUrl, User.class);
	}
	
	public List<UserGuild> getUserGuilds(Credential cred) throws IOException {
		return Arrays.asList(executeQuery(cred, userGuildsQueryUrl, UserGuild[].class));
	}
	
	public <T> T executeQuery(Credential cred, String query, Class<T> clazz) throws IOException {
		final HttpRequestFactory factory = transport.createRequestFactory(cred);
		final HttpResponse resp = factory.buildGetRequest(new GenericUrl(query)).execute();
		return mapper.readValue(resp.getContent(), clazz);
	}
	
}
