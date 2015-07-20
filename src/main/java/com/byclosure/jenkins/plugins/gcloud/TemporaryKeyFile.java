package com.byclosure.jenkins.plugins.gcloud;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;

class TemporaryKeyFile {
	private FilePath tmpKeyFile;

	public TemporaryKeyFile(FilePath configDir, File keyFile) throws IOException, InterruptedException {
		tmpKeyFile = configDir.createTempFile("gcloud", "key");
		tmpKeyFile.copyFrom(new FilePath(keyFile));
	}

	public FilePath getKeyFile() {
		return tmpKeyFile;
	}
}
