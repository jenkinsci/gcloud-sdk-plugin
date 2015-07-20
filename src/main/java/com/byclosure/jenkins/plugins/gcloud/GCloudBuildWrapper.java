package com.byclosure.jenkins.plugins.gcloud;

import com.cloudbees.jenkins.plugins.gcloudsdk.GCloudInstallation;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.DecoratedLauncher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

@RequiresDomain(value = GCloudScopeRequirement.class)
public class GCloudBuildWrapper extends SimpleBuildWrapper {
	private static final Logger LOGGER = Logger.getLogger(GCloudBuildWrapper.class.getName());

	private final String installation;
	private final String credentialsId;

	@DataBoundConstructor
	public GCloudBuildWrapper(String installation, String credentialsId) {
        this.installation = installation;
        this.credentialsId = credentialsId;
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {

        GCloudInstallation sdk = getSDK();
        sdk = sdk.translate(workspace.toComputer().getNode(), initialEnvironment, listener);
        final FilePath configDir = workspace.createTempDir("gcloud", "config");

		final GCloudServiceAccount serviceAccount =
				GCloudServiceAccount.getServiceAccount(build, launcher, listener, credentialsId, configDir);

		if (!serviceAccount.activate(sdk)) {
			configDir.deleteRecursive();
			throw new InterruptedException("Couldn't activate GCloudServiceAccount");
		}

        sdk.buildEnvVars(initialEnvironment);
        for (Map.Entry<String, String> entry : initialEnvironment.entrySet()) {
            context.env(entry.getKey(), entry.getValue());
        }
        context.env("CLOUDSDK_CONFIG", configDir.getRemote());

        context.setDisposer(new GCloudConfigDisposer(configDir));
	}

    public @CheckForNull GCloudInstallation getSDK() {
        GCloudInstallation[] installations = GCloudInstallation.getInstallations();
        for (GCloudInstallation sdk : installations) {
            if (installation.equals(sdk.getName())) return sdk;
        }
        return null;
    }

    public String getInstallation() {
        return installation;
    }

    public String getCredentialsId() {
		return credentialsId;
	}

	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {
		public DescriptorImpl() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "GCloud SDK authentication";
		}

		@Override
		public boolean isApplicable(AbstractProject item) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String serverAddress) {
            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel();
            }
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            MATCHER,
                            CredentialsProvider.lookupCredentials(GoogleRobotPrivateKeyCredentials.class,
                                    project,
                                    ACL.SYSTEM,
                                    URIRequirementBuilder.fromUri(serverAddress).build()));
        }

        public static final CredentialsMatcher MATCHER = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(GoogleRobotPrivateKeyCredentials.class));
	}

    private static class GCloudConfigDisposer extends Disposer {

        private static final long serialVersionUID = 5723296082223871496L;

        private final FilePath configDir;

        public GCloudConfigDisposer(FilePath configDir) {
            this.configDir = configDir;
        }

        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            configDir.deleteRecursive();
        }
    }
}
