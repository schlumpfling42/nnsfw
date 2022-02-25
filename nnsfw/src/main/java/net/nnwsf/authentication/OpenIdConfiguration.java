package net.nnwsf.authentication;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

public class OpenIdConfiguration {

    private Config controllerConfig;
    private Config apiConfig;

    public OpenIdConfiguration(String jsonFileName, String discoveryUri) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> credentialsMap = (Map<String, Object>) new ObjectMapper().readValue(
                new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("credentials.json")),
                Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> credentialParameters =  (Map<String, Object>) credentialsMap.get("web");
        String clientId = (String) credentialParameters.get("client_id");
        String clientSecret =(String) credentialParameters.get("client_secret");
        @SuppressWarnings("unchecked")
        Collection<String> redirectUris = (Collection<String>) credentialParameters.get("redirect_uris");
        init(clientId, clientSecret, redirectUris, discoveryUri);
    }
    public OpenIdConfiguration(String clientId, String clientSecret, Collection<String> redirectUris, String discoveryUri) throws IOException {
        init(clientId, clientSecret, redirectUris, discoveryUri);
    }

    private void init(String clientId, String clientSecret, Collection<String> redirectUris, String discoveryUri) throws IOException {
        OidcConfiguration openIdConfig = new OidcConfiguration();
        openIdConfig.setClientId(clientId);
        openIdConfig.setSecret(clientSecret);
        openIdConfig.setDiscoveryURI(discoveryUri);

        openIdConfig.setResponseType("code");
        openIdConfig.setUseNonce(true);
        openIdConfig.setDisablePkce(true);
        
        OidcClient oidcClient = new OidcClient(openIdConfig);
        final Clients controllerClients = new Clients(redirectUris.iterator().next(), oidcClient);
        controllerConfig = new Config(controllerClients);

        UserInfoOidcAuthenticator authenticator = new UserInfoOidcAuthenticator(openIdConfig);
        HeaderClient headerClient = new HeaderClient("Authorization", "Bearer ", authenticator);

        apiConfig = new Config(headerClient);
    }

    public Config getControllerConfig() {
        return controllerConfig;
    }
    public Config getApiConfig() {
        return apiConfig;
    }
}
