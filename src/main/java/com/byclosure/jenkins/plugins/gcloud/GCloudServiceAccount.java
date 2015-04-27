package com.byclosure.jenkins.plugins.gcloud;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.P12ServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;

public class GCloudServiceAccount {

	private final AbstractBuild build;
	private final Launcher launcher;
	private final BuildListener listener;
	private final String accountId;
	private final TemporaryKeyFile tmpKeyFile;

	public static GCloudServiceAccount getServiceAccount(AbstractBuild build, Launcher launcher,
												   BuildListener listener, String credentialsId) throws IOException, InterruptedException {
		final GoogleRobotPrivateKeyCredentials credential = CredentialsProvider.findCredentialById(
				credentialsId,
				GoogleRobotPrivateKeyCredentials.class,
				build,
				new GCloudScopeRequirement()
		);

		final ServiceAccountConfig serviceAccountConfig = credential.getServiceAccountConfig();

		final String accountId = serviceAccountConfig.getAccountId();
		final File keyFile = getKeyFile(serviceAccountConfig);

		TemporaryKeyFile tmpKeyFile = new TemporaryKeyFile(build, launcher, keyFile);
		tmpKeyFile.copyToTmpDir();

		return new GCloudServiceAccount(build, launcher, listener, accountId, tmpKeyFile);
	}

	private static File getKeyFile(ServiceAccountConfig serviceAccount) {
		String keyFilePath = null;

		if (serviceAccount instanceof JsonServiceAccountConfig) {
			keyFilePath = ((JsonServiceAccountConfig)serviceAccount).getJsonKeyFile();
		} else if (serviceAccount instanceof JsonServiceAccountConfig) {
			keyFilePath = ((P12ServiceAccountConfig)serviceAccount).getP12KeyFile();
		}

		return new File(keyFilePath);
	}

	private GCloudServiceAccount(AbstractBuild build, Launcher launcher, BuildListener listener, String accountId, TemporaryKeyFile tmpKeyFile) {
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
		this.accountId = accountId;
		this.tmpKeyFile = tmpKeyFile;
	}

	boolean activate() throws IOException, InterruptedException {
		final String authCmd = "gcloud auth activate-service-account " + accountId + " --key-file " + tmpKeyFile.getKeyFile().getRemote();

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

	boolean revoke() throws IOException, InterruptedException {
		final String revokeCmd = "gcloud auth revoke " + accountId;

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


	void cleanUp() throws IOException, InterruptedException {
		tmpKeyFile.remove();
	}
}