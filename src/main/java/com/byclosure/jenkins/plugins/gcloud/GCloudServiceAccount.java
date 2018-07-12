package com.byclosure.jenkins.plugins.gcloud;

import com.cloudbees.jenkins.plugins.gcloudsdk.GCloudInstallation;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.jenkins.plugins.credentials.oauth.*;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

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

		if (credential == null) {
			return null;
		}

		final ServiceAccountConfig serviceAccountConfig = credential.getServiceAccountConfig();

		final String accountId = serviceAccountConfig.getAccountId();
		final TemporaryKeyFile tmpKeyFile = getKeyFile(serviceAccountConfig, configDir);

		return new GCloudServiceAccount(launcher, listener, accountId, tmpKeyFile, configDir);
	}

	private static TemporaryKeyFile getKeyFile(ServiceAccountConfig serviceAccount, FilePath configDir) {
		TemporaryKeyFile tmpKeyFile = null;

		try {
			if (serviceAccount instanceof JsonServiceAccountConfig) {
				String keyFilePath = ((JsonServiceAccountConfig)serviceAccount).getJsonKeyFile();
				JsonKey key = JsonKey.load(new JacksonFactory(), new FileInputStream(new File(keyFilePath)));
				tmpKeyFile = new TemporaryKeyFile(configDir, key.toPrettyString());
			} else if (serviceAccount instanceof P12ServiceAccountConfig) {
				String keyFilePath = ((P12ServiceAccountConfig)serviceAccount).getP12KeyFile();
				tmpKeyFile = new TemporaryKeyFile(configDir, keyFilePath);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return tmpKeyFile;
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
		final String authCmd = exec + " auth activate-service-account " + accountId + " --key-file \"" + tmpKeyFile.getKeyFile().getRemote() + "\"";

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

	FilePath getKeyFile(){
    	return tmpKeyFile.getKeyFile();
	}
}