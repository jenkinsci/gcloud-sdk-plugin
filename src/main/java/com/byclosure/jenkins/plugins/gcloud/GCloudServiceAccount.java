package com.byclosure.jenkins.plugins.gcloud;

import com.cloudbees.jenkins.plugins.gcloudsdk.GCloudInstallation;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.P12ServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;

public class GCloudServiceAccount {

	private final Launcher launcher;
	private final TaskListener listener;
	private final String accountId;
	private final TemporaryKeyFile tmpKeyFile;
    private final FilePath configDir;

    public static GCloudServiceAccount getServiceAccount(Run build, Launcher launcher,
														 TaskListener listener, String credentialsId, FilePath configDir) throws IOException, InterruptedException {
		final GoogleRobotPrivateKeyCredentials credential = CredentialsProvider.findCredentialById(
				credentialsId,
				GoogleRobotPrivateKeyCredentials.class,
				build,
				new GCloudScopeRequirement()
		);

		final ServiceAccountConfig serviceAccountConfig = credential.getServiceAccountConfig();

		final String accountId = serviceAccountConfig.getAccountId();
		final File keyFile = getKeyFile(serviceAccountConfig);

		TemporaryKeyFile tmpKeyFile = new TemporaryKeyFile(configDir, keyFile);

		return new GCloudServiceAccount(launcher, listener, accountId, tmpKeyFile, configDir);
	}

	private static File getKeyFile(ServiceAccountConfig serviceAccount) {
		String keyFilePath = null;

		if (serviceAccount instanceof JsonServiceAccountConfig) {
			keyFilePath = ((JsonServiceAccountConfig)serviceAccount).getJsonKeyFile();
		} else if (serviceAccount instanceof P12ServiceAccountConfig) {
			keyFilePath = ((P12ServiceAccountConfig)serviceAccount).getP12KeyFile();
		}

		return new File(keyFilePath);
	}

	private GCloudServiceAccount(Launcher launcher, TaskListener listener, String accountId, TemporaryKeyFile tmpKeyFile, FilePath configDir) {
		this.launcher = launcher;
		this.listener = listener;
		this.accountId = accountId;
		this.tmpKeyFile = tmpKeyFile;
        this.configDir = configDir;
	}

	boolean activate(GCloudInstallation sdk) throws IOException, InterruptedException {
		String exec = "gcloud";
		if (sdk != null) {
			exec = sdk.getExecutable();
		}
		final String authCmd = exec + " auth activate-service-account " + accountId + " --key-file " + tmpKeyFile.getKeyFile().getRemote();

		int retCode = launcher.launch()
				.cmdAsSingleString(authCmd)
                .stdout(listener.getLogger())
                .envs("CLOUDSDK_CONFIG=" + configDir.getRemote())
                .join();

		if (retCode != 0) {
			return false;
		}
		return true;
	}
}