package net.nnwsf.authentication;

import java.util.Set;

import org.pac4j.undertow.account.Pac4jAccount;

public class User {

    private final Pac4jAccount userAccount;

    public User(Pac4jAccount account) {
        this.userAccount = account;
    }

    public String getUsername() {
        return userAccount.getProfile().getUsername();
    }

    public String getEmail() {
        return userAccount.getProfile().getEmail();
    }

    public String getDisplayName() {
        return userAccount.getProfile().getDisplayName();
    }

    public Set<String> getRoles() {
        return userAccount.getRoles();
    }
    
}
