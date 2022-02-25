package net.nnwsf.authentication;

import java.util.Set;

import org.pac4j.core.profile.UserProfile;

public class User {

    private final UserProfile profile;

    public User(UserProfile profile) {
        this.profile = profile;
    }

    public String getUsername() {
        return profile.getUsername();
    }

    public String getEmail() {
        return (String)profile.getAttribute("email");
    }

    public String getDisplayName() {
        return (String)profile.getAttribute("name");
    }

    public Set<String> getRoles() {
        return profile.getRoles();
    }
    
}
