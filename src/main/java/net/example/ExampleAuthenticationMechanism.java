package net.example;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import net.nnwsf.authentication.AuthenticationMechanism;
import net.nnwsf.authentication.GoogleAuthenticationMechanism;

@AuthenticationMechanism
public class ExampleAuthenticationMechanism extends GoogleAuthenticationMechanism {

    protected String getAuthToken(HttpServerExchange exchange) {
        Cookie authCookie = exchange.getRequestCookies().get("authToken");
        if(authCookie != null) {
            return authCookie.getValue();
        }
        return null;
    }

    protected String getRedirectUrl(HttpServerExchange exchange) {
        return "http://lvh.me:8080/login";
    }


}
