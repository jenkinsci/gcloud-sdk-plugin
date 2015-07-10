package com.cloudbees.jenkins.plugins.gcloudsdk;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GCloudInstaller extends DownloadFromUrlInstaller {

    @DataBoundConstructor
    public GCloudInstaller(String id) {
        super(id);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath installation = super.performInstallation(tool, node, log);

        Launcher launcher = node.createLauncher(log);
            launcher.launch()
                .stdout(log)
                .cmds( installation.child(launcher.isUnix() ? "install.sh" : "install.bat").getRemote(),
                        "--usage-reporting=false", "--path-update=false", "--bash-completion=false")
                .join();

        return installation;
    }

    @Override
    public Installable getInstallable() throws IOException {
        return SDK;
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<GCloudInstaller> {
        public String getDisplayName() {
            return "Install from google.com";
        }

        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            return Collections.singletonList(SDK);
        }
    }


    public static Installable SDK = new Installable() {
        {
            id = "google-cloud-sdk";
            url = "https://dl.google.com/dl/cloudsdk/release/google-cloud-sdk.zip";
            name = "latest";
        }
    };
}
