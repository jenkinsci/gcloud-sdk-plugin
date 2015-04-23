package com.byclosure.jenkins.plugins.gcloud;

import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;

import java.util.Collection;

public class GCloudScopeRequirement extends GoogleOAuth2ScopeRequirement {
	@Override
	public Collection<String> getScopes() {
		return null;
	}
}
