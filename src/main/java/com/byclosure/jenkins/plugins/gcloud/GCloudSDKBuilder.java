package com.byclosure.jenkins.plugins.gcloud;


import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.P12ServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

@RequiresDomain(value = GCloudScopeRequirement.class)
public class GCloudSDKBuilder extends Builder {

	private final String credentialsId;
	private final String command;

	@DataBoundConstructor
	public GCloudSDKBuilder(String credentialsId, String command) {
		this.credentialsId = credentialsId;
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		final GCloudServiceAccount serviceAccount =
				GCloudServiceAccount.getServiceAccount(build, launcher, listener, credentialsId);

		if (!serviceAccount.activate()) {
			serviceAccount.cleanUp();
			return false;
		}

		if (!executeGCloudCLI(build, launcher, listener)) {
			serviceAccount.revoke();
			serviceAccount.cleanUp();
			return false;
		}

		if (!serviceAccount.revoke()) {
			serviceAccount.cleanUp();
			return false;
		}

		serviceAccount.cleanUp();
		return true;
	}

	private boolean executeGCloudCLI(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		int retCode = launcher.launch()
				.pwd(build.getWorkspace())
				.cmdAsSingleString("gcloud " + command)
				.stdout(listener.getLogger())
				.join();

		if (retCode != 0) {
			return false;
		}
		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			load();
		}

		public FormValidation doCheckCommand(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a command");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Execute gcloud CLI";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}


}

