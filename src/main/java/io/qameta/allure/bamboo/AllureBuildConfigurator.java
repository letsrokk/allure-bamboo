package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plan.configuration.MiscellaneousPlanConfigurationPlugin;
import com.atlassian.bamboo.specs.api.builders.allure.AllureSpecs;
import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.atlassian.bamboo.specs.api.model.allure.AllureSpecsProperties;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;
import com.atlassian.bamboo.specs.yaml.BambooYamlParserUtils;
import com.atlassian.bamboo.specs.yaml.MapNode;
import com.atlassian.bamboo.specs.yaml.Node;
import com.atlassian.bamboo.specs.yaml.StringNode;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.bamboo.v2.build.ImportExportAwarePlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.collect.Sets;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
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
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class AllureBuildConfigurator extends BaseConfigurablePlugin
        implements MiscellaneousPlanConfigurationPlugin, ImportExportAwarePlugin<AllureSpecs, AllureSpecsProperties> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildConfigurator.class);
    private static final ComparableVersion BAMBOO_MIN_VERSION_FOR_YAML_SUPPORT = new ComparableVersion("7.2.1");

    final private ApplicationProperties applicationProperties;
    final private BambooExecutablesManager executablesManager;
    final private AllureSettingsManager settingsManager;

    public AllureBuildConfigurator(ApplicationProperties applicationProperties, AllureSettingsManager settingsManager, BambooExecutablesManager executablesManager) {
        this.applicationProperties = applicationProperties;
        this.settingsManager = settingsManager;
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

    private Boolean getDefaultEnabled() {
        return ofNullable(settingsManager)
                .map(AllureSettingsManager::getSettings)
                .map(AllureGlobalConfig::isEnabledByDefault)
                .orElse(FALSE);
    }

    private Boolean getDefaultFailedOnly() {
        return TRUE;
    }

    private String getDefaultExecutable() {
        return ofNullable(executablesManager)
                .flatMap(BambooExecutablesManager::getDefaultAllureExecutable)
                .orElse(null);
    }

    private String getDefaultArtifactName() {
        return "";
    }

    @Override
    public void prepareConfigObject(@NotNull BuildConfiguration buildConfiguration) {
        super.prepareConfigObject(buildConfiguration);
        if (buildConfiguration.getProperty(ALLURE_CONFIG_ENABLED) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_ENABLED, getDefaultEnabled());
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_FAILED_ONLY) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_FAILED_ONLY, getDefaultFailedOnly());
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_EXECUTABLE) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_EXECUTABLE, getDefaultExecutable());
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_ARTIFACT_NAME) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_ARTIFACT_NAME, getDefaultArtifactName());
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
    public AllureSpecs toSpecsEntity(@NotNull HierarchicalConfiguration buildConfiguration) {
        return new AllureSpecs(
                buildConfiguration.getBoolean(ALLURE_CONFIG_ENABLED),
                buildConfiguration.getBoolean(ALLURE_CONFIG_FAILED_ONLY),
                buildConfiguration.getString(ALLURE_CONFIG_EXECUTABLE),
                buildConfiguration.getString(ALLURE_CONFIG_ARTIFACT_NAME)
        );
    }

    @Override
    public void addToBuildConfiguration(@NotNull AllureSpecsProperties specProperties,
                                        @NotNull HierarchicalConfiguration buildConfiguration) {
        specProperties.validate();
        buildConfiguration.setProperty(ALLURE_CONFIG_ENABLED, specProperties.getEnabled());
        buildConfiguration.setProperty(ALLURE_CONFIG_FAILED_ONLY, specProperties.getFailedOnly());
        buildConfiguration.setProperty(ALLURE_CONFIG_EXECUTABLE, specProperties.getExecutable());
        buildConfiguration.setProperty(ALLURE_CONFIG_ARTIFACT_NAME, specProperties.getArtifactName());
    }

    private ComparableVersion getBambooVersion() {
        String bambooVersion =
                ofNullable(applicationProperties).map(ApplicationProperties::getVersion).orElse("0.0.0");
        return new ComparableVersion(bambooVersion);
    }

    private boolean isYamlImportExportSupported() {
        return getBambooVersion().compareTo(BAMBOO_MIN_VERSION_FOR_YAML_SUPPORT) >= 0;
    }

    @Nullable
    @Override
    public AllureSpecs fromYaml(Node node) throws PropertiesValidationException {
        if (isYamlImportExportSupported()) {
            if (node instanceof MapNode) {
                MapNode mapNode = (MapNode) node;
                final Optional<MapNode> allureOptional = mapNode.getOptionalMap(YamlTags.YAML_ALLURE_ROOT);
                if (allureOptional.isPresent()) {
                    AllureSpecs allureSpecs = new AllureSpecs();
                    MapNode allureNode = allureOptional.get();

                    Optional<StringNode> enabledOptional = allureNode.getOptionalString(YamlTags.YAML_ENABLED);
                    if (enabledOptional.isPresent() && StringUtils.isNotBlank(enabledOptional.get().get())) {
                        allureSpecs.enabled(Boolean.parseBoolean(enabledOptional.get().get()));
                    } else {
                        allureSpecs.enabled(getDefaultEnabled());
                    }
                    Optional<StringNode> failedOnlyOptional = allureNode.getOptionalString(YamlTags.YAML_FAILED_ONLY);
                    if (failedOnlyOptional.isPresent() && StringUtils.isNotBlank(failedOnlyOptional.get().get())) {
                        allureSpecs.failedOnly(Boolean.parseBoolean(failedOnlyOptional.get().get()));
                    } else {
                        allureSpecs.failedOnly(getDefaultFailedOnly());
                    }
                    Optional<StringNode> executableOptional = allureNode.getOptionalString(YamlTags.YAML_EXECUTABLE);
                    if (executableOptional.isPresent() && StringUtils.isNotBlank(executableOptional.get().get())) {
                        allureSpecs.executable(executableOptional.get().get());
                    } else {
                        allureSpecs.executable(getDefaultExecutable());
                    }
                    Optional<StringNode> artifactNameOptional = allureNode.getOptionalString(YamlTags.YAML_ARTIFACT_NAME);
                    if (artifactNameOptional.isPresent() && StringUtils.isNotBlank(artifactNameOptional.get().get())) {
                        allureSpecs.artifactName(artifactNameOptional.get().get());
                    } else {
                        allureSpecs.artifactName(getDefaultArtifactName());
                    }

                    return allureSpecs;
                }
            }
        } else  {
            LOGGER.info("Bamboo Version {} < {}. Import from YAML Specs is not supported",
                    getBambooVersion(),
                    BAMBOO_MIN_VERSION_FOR_YAML_SUPPORT);
        }
        return null;
    }

    @Nullable
    @Override
    public Node toYaml(@NotNull AllureSpecsProperties specsProperties) {
        if (isYamlImportExportSupported()) {
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
        } else {
            LOGGER.info("Bamboo Version {} < {}. Export to YAML Specs is not supported",
                    getBambooVersion(),
                    BAMBOO_MIN_VERSION_FOR_YAML_SUPPORT);
            return null;
        }
    }

    private interface YamlTags {
        String YAML_ALLURE_ROOT = "allure";
        String YAML_ENABLED = "enabled";
        String YAML_FAILED_ONLY = "failed-only";
        String YAML_EXECUTABLE = "executable";
        String YAML_ARTIFACT_NAME = "artifact-name";
    }
}
