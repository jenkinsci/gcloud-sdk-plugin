package com.cloudbees.jenkins.plugins.gcloudsdk;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GCloudInstaller extends DownloadFromUrlInstaller {


    private String additionalComponents;

    @DataBoundConstructor
    public GCloudInstaller(String id, String additionalComponents) {
        super(id);
        this.additionalComponents = additionalComponents;
    }

    public String getAdditionalComponents() {
        return additionalComponents;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath installation = super.performInstallation(tool, node, log);

        Launcher launcher = node.createLauncher(log);

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(installation.child(launcher.isUnix() ? "install.sh" : "install.bat").getRemote())
            .add("--usage-reporting=false", "--path-update=false", "--bash-completion=false");
        if (StringUtils.isNotBlank(additionalComponents))
            args.add("--additional-components", additionalComponents);

            launcher.launch()
                .stdout(log)
                .cmds(args.toCommandArray())
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
