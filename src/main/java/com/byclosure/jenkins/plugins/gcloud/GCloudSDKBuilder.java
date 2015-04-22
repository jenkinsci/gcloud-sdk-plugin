package com.byclosure.jenkins.plugins.gcloud;

import hudson.Launcher;
import hudson.Extension;
import hudson.Proc;
import hudson.remoting.RemoteOutputStream;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

public class GCloudSDKBuilder extends Builder {

	private final String command;

	@DataBoundConstructor
	public GCloudSDKBuilder(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		final int retCode = launcher.launch()
				.pwd(build.getWorkspace())
				.cmdAsSingleString("gcloud " + command)
				.stdout(listener.getLogger())
				.join();

		if (retCode == 0) {
			return true;
		}

		return false;
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
			return "Execute gcloud CLI";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}
}

