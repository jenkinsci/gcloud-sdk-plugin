package com.byclosure.jenkins.plugins.gcloud;

import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@RequiresDomain(value = GCloudScopeRequirement.class)
public class GCloudBuildWrapper extends BuildWrapper {
	private static final Logger LOGGER = Logger.getLogger(GCloudBuildWrapper.class.getName());
	private final String credentialsId;

	@DataBoundConstructor
	public GCloudBuildWrapper(String credentialsId) {
		this.credentialsId = credentialsId;
	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		final GCloudServiceAccount serviceAccount =
				GCloudServiceAccount.getServiceAccount(build, launcher, listener, credentialsId);

		if (!serviceAccount.activate()) {
			serviceAccount.cleanUp();
			throw new InterruptedException("Couldn't activate GCloudServiceAccount");
		}

		return new Environment() {
			@Override
			public void buildEnvVars(Map<String, String> env) {
			}

			@Override
			public boolean tearDown(AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
				if (!serviceAccount.revoke()) {
					serviceAccount.cleanUp();
					return false;
				}

				serviceAccount.cleanUp();
				return true;
			}

		};
	}

	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public String getDisplayName() {
			return "GCloud authentication";
		}

		@Override
		public boolean isApplicable(AbstractProject item) {
			return true;
		}

	}
}
