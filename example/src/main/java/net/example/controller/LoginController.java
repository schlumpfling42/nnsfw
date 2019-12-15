package net.example.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Deque;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import net.nnwsf.controller.Controller;
import net.nnwsf.controller.Get;

@Controller("/login")
public class LoginController {
    final private GoogleClientSecrets clientSecrets;
    private final JacksonFactory jsonFactory = new JacksonFactory();
    private MemoryDataStoreFactory dataStoreFactory = new MemoryDataStoreFactory();

    public LoginController() {
        try {
            clientSecrets = GoogleClientSecrets.load(jsonFactory,
                    new InputStreamReader(LoginController.class.getClassLoader().getResourceAsStream("credentials.json")));
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load the google oauth credentials", ioe);
        }

    }

    @Get("/")
    public String get(HttpServerExchange exchange) {
        try
        {
            String code = exchange.getQueryParameters().get("code").element();
            Deque<String> redirectUrl = exchange.getQueryParameters().get("state");
            TokenResponse tokenResponse = getToken(code);

            exchange.getResponseCookies().put("authToken", new CookieImpl("authToken", tokenResponse.getAccessToken()));
            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
            if(redirectUrl != null && !redirectUrl.isEmpty())
            exchange.getResponseHeaders().put(Headers.LOCATION, redirectUrl.element() );
            exchange.endExchange();

            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.endExchange();
        return null;
    }
    protected TokenResponse getToken(String code) throws IOException {


        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), jsonFactory, clientSecrets,
                Collections.singleton("https://www.googleapis.com/auth/userinfo.email")).setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize

        final TokenResponse tokenResponse =
                flow.newTokenRequest(code)
                        .setRedirectUri("http://lvh.me:8080/login")
                        .execute();
        flow.createAndStoreCredential(tokenResponse, clientSecrets.getDetails().getClientId());
        return tokenResponse;
    }


}