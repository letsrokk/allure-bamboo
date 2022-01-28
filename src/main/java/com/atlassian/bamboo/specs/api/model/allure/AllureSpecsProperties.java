package com.atlassian.bamboo.specs.api.model.allure;

import com.atlassian.bamboo.specs.api.builders.allure.AllureSpecs;
import com.atlassian.bamboo.specs.api.codegen.annotations.Builder;
import com.atlassian.bamboo.specs.api.model.AtlassianModuleProperties;
import com.atlassian.bamboo.specs.api.model.plan.configuration.PluginConfigurationProperties;
import com.atlassian.bamboo.specs.api.validators.common.ImporterUtils;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
@Builder(AllureSpecs.class)
public class AllureSpecsProperties implements PluginConfigurationProperties {

    public static final ValidationContext VALIDATION_CONTEXT = ValidationContext.of("Allure Config");

    private Boolean enabled;
    private Boolean failedOnly;
    private String executable;
    private String artifactName;

    private AllureSpecsProperties() {
    }

    public AllureSpecsProperties(Boolean enabled, Boolean failedOnly, String executable, String artifactName) {
        this.enabled = enabled;
        this.failedOnly = failedOnly;
        this.executable = executable;
        this.artifactName = artifactName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getFailedOnly() {
        return failedOnly;
    }

    public void setFailedOnly(Boolean failedOnly) {
        this.failedOnly = failedOnly;
    }

    public String getExecutable() {
        return this.executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    @Override
    public AtlassianModuleProperties getAtlassianPlugin() {
        return new AtlassianModuleProperties("io.qameta.allure.allure-bamboo:allureConfig");
    }

    @Override
    public void validate() {
        ImporterUtils.checkNotNull(VALIDATION_CONTEXT, "enabled", this.enabled);
        ImporterUtils.checkNotNull(VALIDATION_CONTEXT, "failedOnly", this.failedOnly);
        ImporterUtils.checkNotBlank(VALIDATION_CONTEXT, "executable", this.executable);
        ImporterUtils.checkNotNull(VALIDATION_CONTEXT, "artifactName", this.artifactName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllureSpecsProperties that = (AllureSpecsProperties) o;
        return Objects.equals(enabled, that.enabled) && Objects.equals(failedOnly, that.failedOnly) && Objects.equals(executable, that.executable) && Objects.equals(artifactName, that.artifactName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, failedOnly, executable, artifactName);
    }
}
