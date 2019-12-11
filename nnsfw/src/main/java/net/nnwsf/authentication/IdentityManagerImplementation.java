package net.nnwsf.authentication;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

public class IdentityManagerImplementation implements IdentityManager {
	@Override
	public Account verify(String id, Credential credential) {
	    return null;
	}

	@Override
	public Account verify(Credential credential) {
	    return null;
	}

	@Override
	public Account verify(Account account) {
	    return null;
	}
}