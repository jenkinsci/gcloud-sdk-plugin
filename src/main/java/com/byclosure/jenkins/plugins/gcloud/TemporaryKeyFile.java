package com.byclosure.jenkins.plugins.gcloud;

import hudson.FilePath;

import java.io.IOException;

class TemporaryKeyFile {
	private FilePath tmpKeyFile;

	public TemporaryKeyFile(FilePath configDir, String content) throws IOException, InterruptedException {
		tmpKeyFile = configDir.createTempFile("gcloud", "key");
		tmpKeyFile.write(content, "UTF-8");
	}

	public TemporaryKeyFile(FilePath configDir, FilePath file) throws IOException, InterruptedException {
		tmpKeyFile = configDir.createTempFile("gcloud", "key");
		tmpKeyFile.copyFrom(file);
	}

	public FilePath getKeyFile() {
		return tmpKeyFile;
	}
}
