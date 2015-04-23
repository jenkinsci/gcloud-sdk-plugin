package com.byclosure.jenkins.plugins.gcloud;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.*;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.*;

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

	private ServiceAccountConfig getServiceAccountConfig(AbstractBuild build) {
		final GoogleRobotPrivateKeyCredentials credential = CredentialsProvider.findCredentialById(
				credentialsId,
				GoogleRobotPrivateKeyCredentials.class,
				build,
				new GCloudScopeRequirement()
		);

		return credential.getServiceAccountConfig();
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		final ServiceAccountConfig serviceAccount = getServiceAccountConfig(build);

		final String accountId = serviceAccount.getAccountId();
		final File keyFile = getKeyFile(serviceAccount);

		FilePath tmpDir = build.getWorkspace().createTempDir("gcloud", null);
		FilePath tmpKeyFile = new FilePath(launcher.getChannel(),
				new File(tmpDir.getRemote(), keyFile.getName()).getPath());
		tmpKeyFile.copyFrom(new FilePath(keyFile));

		if (!addCredentials(build, launcher, listener, accountId, tmpKeyFile, tmpDir)) {
			removeTmpDir(tmpDir);
			return false;
		}

		if (!executeGCloudCLI(build, launcher, listener)) {
			revokeCredentials(build, launcher, listener, accountId);
			removeTmpDir(tmpDir);
			return false;
		}

		if (!revokeCredentials(build, launcher, listener, accountId)) {
			removeTmpDir(tmpDir);
			return false;
		}

		removeTmpDir(tmpDir);
		return true;
	}

	private File getKeyFile(ServiceAccountConfig serviceAccount) {
		String keyFilePath = null;

		if (serviceAccount instanceof JsonServiceAccountConfig) {
			keyFilePath = ((JsonServiceAccountConfig)serviceAccount).getJsonKeyFile();
		} else if (serviceAccount instanceof JsonServiceAccountConfig) {
			keyFilePath = ((P12ServiceAccountConfig)serviceAccount).getP12KeyFile();
		}

		return new File(keyFilePath);
	}

	private boolean addCredentials(AbstractBuild build, Launcher launcher, BuildListener listener, String accountId, FilePath tmpKeyFile, FilePath tmpDir) throws IOException, InterruptedException {
		final String authCmd = "gcloud auth activate-service-account " + accountId + " --key-file " + tmpKeyFile.getRemote();

		int retCode = launcher.launch()
				.pwd(build.getWorkspace())
				.cmdAsSingleString(authCmd)
				.stdout(listener.getLogger())
				.join();

		if (retCode != 0) {
			return false;
		}
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

	private boolean revokeCredentials(AbstractBuild build, Launcher launcher, BuildListener listener, String accountId) throws IOException, InterruptedException {
		final String revokeCmd = "gcloud auth revoke " + accountId ;

		int retCode = launcher.launch()
				.pwd(build.getWorkspace())
				.cmdAsSingleString(revokeCmd)
				.stdout(listener.getLogger())
				.join();

		if (retCode != 0) {
			return false;
		}
		return true;
	}

	private void removeTmpDir(FilePath tmpDir) throws IOException, InterruptedException {
		if(Computer.currentComputer() instanceof SlaveComputer) {
			tmpDir.deleteRecursive();
		}
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

