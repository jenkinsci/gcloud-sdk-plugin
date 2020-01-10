package com.byclosure.jenkins.plugins.gcloud;


import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import hudson.Extension;
import hudson.FilePath;
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
import java.io.IOException;

@RequiresDomain(value = GCloudScopeRequirement.class)
public class GCloudSDKWithAuthBuilder extends Builder {

	private final String credentialsId;
	private final String command;

	@DataBoundConstructor
	public GCloudSDKWithAuthBuilder(String credentialsId, String command) {
		this.credentialsId = credentialsId;
		this.command = command;
	}

	public String getCredentialsId() {
		return credentialsId;
	}


	public String getCommand() {
		return command;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		final FilePath ws = build.getWorkspace();
		if (ws == null) throw new RuntimeException("Unable to get build workspace.");

		final FilePath configDir = ws.createTempDir("gcloud", "config");
		final GCloudServiceAccount serviceAccount =
				GCloudServiceAccount.getServiceAccount(build, launcher, listener, credentialsId, configDir);

		try {
			if (serviceAccount == null) {
				return false;
			}

			if (!serviceAccount.activate(null)) {
				return false;
			}

			if (!executeGCloudCLI(build, launcher, listener, configDir)) {
				return false;
			}
			return true;
		} finally {
			configDir.deleteRecursive();
		}
	}

	private boolean executeGCloudCLI(AbstractBuild build, Launcher launcher, BuildListener listener, FilePath configDir) throws IOException, InterruptedException {
		int retCode = launcher.launch()
				.pwd(build.getWorkspace())
				.cmdAsSingleString("gcloud " + command)
				.stdout(listener.getLogger())
                .envs("CLOUDSDK_CONFIG=" + configDir.getRemote())
                .join();

		return retCode == 0;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
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
			return "Execute gcloud CLI (with auth)";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}


}

