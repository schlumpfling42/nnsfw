package net.nnwsf.authentication;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.BrowserClientRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public abstract class GoogleAuthenticationMechanism implements AuthenticationMechanism {

    final private GoogleClientSecrets clientSecrets;

    public GoogleAuthenticationMechanism() {
        try {
            clientSecrets = GoogleClientSecrets.load(new GsonFactory(),
                    new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("credentials.json")));
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load the google oauth credentials", ioe);
        }
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        String authToken = getAuthToken(exchange);
        if (authToken == null) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        } else {
            try {
                Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                        .setAccessToken(authToken);
                Oauth2 oauth2 = new Oauth2.Builder(new NetHttpTransport(), new GsonFactory(), credential)
                        .setApplicationName("Oauth2").build();
                Userinfo userinfo = oauth2.userinfo().get().execute();
                Account account = new Account() {
                    /**
                     *
                     */
                    private static final long serialVersionUID = 840979126383464426L;

                    @Override
                    public Principal getPrincipal() {
                        return new Principal() {
                            @Override
                            public String getName() {
                                return userinfo.getEmail();
                            }
                        };
                    }

                    @Override
                    public Set<String> getRoles() {
                        return null;
                    }
                };
                securityContext.authenticationComplete(account, "GoogleOauth2", false);
                return AuthenticationMechanismOutcome.AUTHENTICATED;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
    }

    protected abstract String getAuthToken(HttpServerExchange exchange);

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        try {
            GoogleClientSecrets.Details details = clientSecrets.getDetails();
            String url = new BrowserClientRequestUrl(details.getAuthUri(), details.getClientId())
                    .setScopes(Arrays.asList("https://www.googleapis.com/auth/userinfo.email"))
                    .setRedirectUri(getRedirectUrl(exchange)).setState(exchange.getRequestPath())
                    .setResponseTypes(Collections.singleton("code")).build();

            exchange.getResponseHeaders().put(Headers.REFERER, exchange.getRequestURL());
            exchange.getResponseHeaders().put(Headers.LOCATION, url);
            return new ChallengeResult(true, StatusCodes.TEMPORARY_REDIRECT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getRedirectUrl(HttpServerExchange exchange);
}
