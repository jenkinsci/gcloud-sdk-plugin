package com.cloudbees.jenkins.plugins.gcloudsdk;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GCloudInstallation extends ToolInstallation implements NodeSpecific<GCloudInstallation>, EnvironmentSpecific<GCloudInstallation> {

    @DataBoundConstructor
    public GCloudInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }


    public void buildEnvVars(EnvVars env) {
        env.override("PATH+GCLOUD", getHome()+"/bin");
        env.override("CLOUDSDK_DIR", getHome());
        env.override("CLOUDSDK_PYTHON_SITEPACKAGES", "1");
    }


    @Override
    public GCloudInstallation translate(Node node, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
        return (GCloudInstallation) super.translate(node, envs, listener);
    }

    public GCloudInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new GCloudInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    public GCloudInstallation forEnvironment(EnvVars environment) {
        return new GCloudInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public static GCloudInstallation[] getInstallations() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class).getInstallations();
    }

    public String getExecutable() {
        return getHome()+"/bin/gcloud";
    }

    @Extension @Symbol("gcloud")
    public static class DescriptorImpl extends ToolDescriptor<GCloudInstallation> {

        @Override
        public String getDisplayName() {
            return "Google Cloud SDK";
        }

        public DescriptorImpl() {
            load();
        }

        public void setInstallations(GCloudInstallation... installations) {
            super.setInstallations(installations);
            save();
        }

    }
}
