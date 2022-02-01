package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plan.configuration.MiscellaneousPlanConfigurationPlugin;
import com.atlassian.bamboo.specs.api.builders.allure.AllureSettings;
import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.atlassian.bamboo.specs.api.model.allure.AllureSettingsProperties;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;
import com.atlassian.bamboo.specs.yaml.BambooYamlParserUtils;
import com.atlassian.bamboo.specs.yaml.MapNode;
import com.atlassian.bamboo.specs.yaml.Node;
import com.atlassian.bamboo.specs.yaml.StringNode;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.bamboo.v2.build.ImportExportAwarePlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.google.common.collect.Sets;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.atlassian.bamboo.plan.PlanClassHelper.isChain;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ARTIFACT_NAME;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isEmpty;

@SuppressWarnings("unchecked")
public class AllureBuildConfigurator extends BaseConfigurablePlugin
        implements MiscellaneousPlanConfigurationPlugin, ImportExportAwarePlugin<AllureSettings, AllureSettingsProperties> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildConfigurator.class);

    private BambooExecutablesManager executablesManager;

    private AllureSettingsManager settingsManager;

    public void setSettingsManager(AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void setExecutablesManager(BambooExecutablesManager executablesManager) {
        this.executablesManager = executablesManager;
    }

    @Override
    public boolean isApplicableTo(@NotNull final ImmutablePlan plan) {
        return isChain(plan);
    }

    @NotNull
    @Override
    public ErrorCollection validate(@NotNull final BuildConfiguration buildConfiguration) {
        final ErrorCollection collection = super.validate(buildConfiguration);
        if (buildConfiguration.getBoolean(ALLURE_CONFIG_ENABLED)) {
            if (isEmpty(buildConfiguration.getString(ALLURE_CONFIG_EXECUTABLE))) {
                collection.addError(ALLURE_CONFIG_EXECUTABLE, "Cannot be empty!");
            }
        }
        return collection;
    }

    @Override
    public void prepareConfigObject(@NotNull BuildConfiguration buildConfiguration) {
        super.prepareConfigObject(buildConfiguration);
        if (buildConfiguration.getProperty(ALLURE_CONFIG_ENABLED) == null) {
            ofNullable(settingsManager)
                    .map(AllureSettingsManager::getSettings)
                    .ifPresent(settings -> buildConfiguration.setProperty(ALLURE_CONFIG_ENABLED, settings.isEnabledByDefault()));
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_FAILED_ONLY) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_FAILED_ONLY, TRUE);
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_EXECUTABLE) == null) {
            ofNullable(executablesManager)
                    .flatMap(BambooExecutablesManager::getDefaultAllureExecutable)
                    .ifPresent(executable -> buildConfiguration.setProperty(ALLURE_CONFIG_EXECUTABLE, executable));
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_ARTIFACT_NAME) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_ARTIFACT_NAME, "");
        }
    }

    @NotNull
    @Override
    public Set<String> getConfigurationKeys() {
        return Sets.newHashSet(
                ALLURE_CONFIG_ENABLED,
                ALLURE_CONFIG_FAILED_ONLY,
                ALLURE_CONFIG_EXECUTABLE,
                ALLURE_CONFIG_ARTIFACT_NAME
        );
    }

    @NotNull
    @Override
    public AllureSettings toSpecsEntity(@NotNull HierarchicalConfiguration buildConfiguration) {
        return new AllureSettings(
                buildConfiguration.getBoolean(ALLURE_CONFIG_ENABLED),
                buildConfiguration.getBoolean(ALLURE_CONFIG_FAILED_ONLY),
                buildConfiguration.getString(ALLURE_CONFIG_EXECUTABLE),
                buildConfiguration.getString(ALLURE_CONFIG_ARTIFACT_NAME)
        );
    }

    @Override
    public void addToBuildConfiguration(@NotNull AllureSettingsProperties specProperties,
                                        @NotNull HierarchicalConfiguration buildConfiguration) {
        specProperties.validate();
        buildConfiguration.setProperty(ALLURE_CONFIG_ENABLED, specProperties.getEnabled());
        buildConfiguration.setProperty(ALLURE_CONFIG_FAILED_ONLY, specProperties.getFailedOnly());
        buildConfiguration.setProperty(ALLURE_CONFIG_EXECUTABLE, specProperties.getExecutable());
        buildConfiguration.setProperty(ALLURE_CONFIG_ARTIFACT_NAME, specProperties.getArtifactName());
    }

    @Nullable
    @Override
    public AllureSettings fromYaml(Node node) throws PropertiesValidationException {
        if (node instanceof MapNode) {
            MapNode mapNode = (MapNode) node;
            final Optional<MapNode> allureOptional = mapNode.getOptionalMap(YamlTags.YAML_ALLURE_ROOT);
            if (allureOptional.isPresent()) {
                AllureSettings allureSettings = new AllureSettings();
                MapNode allureNode = allureOptional.get();

                Optional<StringNode> enabledOptional = allureNode.getOptionalString(YamlTags.YAML_ENABLED);
                if (enabledOptional.isPresent() && StringUtils.isNotBlank(enabledOptional.get().get())) {
                    allureSettings.enabled(Boolean.parseBoolean(enabledOptional.get().get()));
                } else {
                    ofNullable(settingsManager)
                            .map(AllureSettingsManager::getSettings)
                            .ifPresent(settings -> allureSettings.enabled(settings.isEnabledByDefault()));
                }
                Optional<StringNode> failedOnlyOptional = allureNode.getOptionalString(YamlTags.YAML_FAILED_ONLY);
                if (failedOnlyOptional.isPresent() && StringUtils.isNotBlank(failedOnlyOptional.get().get())) {
                    allureSettings.failedOnly(Boolean.parseBoolean(failedOnlyOptional.get().get()));
                } else {
                    allureSettings.failedOnly(TRUE);
                }
                Optional<StringNode> executableOptional = allureNode.getOptionalString(YamlTags.YAML_EXECUTABLE);
                if (executableOptional.isPresent() && StringUtils.isNotBlank(executableOptional.get().get())) {
                    allureSettings.executable(executableOptional.get().get());
                } else {
                    ofNullable(executablesManager)
                            .flatMap(BambooExecutablesManager::getDefaultAllureExecutable)
                            .ifPresent(allureSettings::executable);
                }
                Optional<StringNode> artifactNameOptional = allureNode.getOptionalString(YamlTags.YAML_ARTIFACT_NAME);
                if (artifactNameOptional.isPresent() && StringUtils.isNotBlank(artifactNameOptional.get().get())) {
                    allureSettings.artifactName(artifactNameOptional.get().get());
                } else {
                    allureSettings.artifactName("");
                }

                return allureSettings;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Node toYaml(@NotNull AllureSettingsProperties specsProperties) {
        final Map<String, Object> allure = new HashMap<>();
        allure.put(YamlTags.YAML_ENABLED, specsProperties.getEnabled());
        allure.put(YamlTags.YAML_FAILED_ONLY, specsProperties.getFailedOnly());
        allure.put(YamlTags.YAML_EXECUTABLE, specsProperties.getExecutable());
        if (StringUtils.isNotBlank(specsProperties.getArtifactName())) {
            allure.put(YamlTags.YAML_ARTIFACT_NAME, specsProperties.getArtifactName());
        }

        final Map<String, Map<String, Object>> result = new HashMap<>();
        result.put(YamlTags.YAML_ALLURE_ROOT, allure);

        return BambooYamlParserUtils.asNode(result, ValidationContext.of(YamlTags.YAML_ALLURE_ROOT));
    }

    private static class YamlTags {
        static String YAML_ALLURE_ROOT = "allure";
        static String YAML_ENABLED = "enabled";
        static String YAML_FAILED_ONLY = "failed-only";
        static String YAML_EXECUTABLE = "executable";
        static String YAML_ARTIFACT_NAME = "artifact-name";
    }
}
