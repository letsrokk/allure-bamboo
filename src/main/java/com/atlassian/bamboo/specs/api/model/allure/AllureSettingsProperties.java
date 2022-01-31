package com.atlassian.bamboo.specs.api.model.allure;

import com.atlassian.bamboo.specs.api.codegen.annotations.Builder;
import com.atlassian.bamboo.specs.api.model.AtlassianModuleProperties;
import com.atlassian.bamboo.specs.api.model.plan.configuration.PluginConfigurationProperties;
import com.atlassian.bamboo.specs.api.validators.common.ImporterUtils;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;
import com.atlassian.bamboo.specs.api.builders.allure.AllureSettings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

@Immutable
@Builder(AllureSettings.class)
public class AllureSettingsProperties implements PluginConfigurationProperties {

    public static final ValidationContext VALIDATION_CONTEXT = ValidationContext.of("Allure Config");

    private Boolean enabled;
    private Boolean failedOnly;
    private String executable;

    private AllureSettingsProperties() {
    }

    public AllureSettingsProperties(Boolean enabled, Boolean failedOnly, String executable) {
        this.enabled = enabled;
        this.failedOnly = failedOnly;
        this.executable = executable;
        validate();
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

    @Override
    public AtlassianModuleProperties getAtlassianPlugin() {
        return new AtlassianModuleProperties("io.qameta.allure.allure-bamboo:allureConfig");
    }

    @Override
    public void validate() {
        ImporterUtils.checkNotNull(VALIDATION_CONTEXT, "enabled", this.enabled);
        ImporterUtils.checkNotNull(VALIDATION_CONTEXT, "failedOnly", this.enabled);
        ImporterUtils.checkNotNull(VALIDATION_CONTEXT, "executable", this.enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllureSettingsProperties that = (AllureSettingsProperties) o;
        return Objects.equals(enabled, that.enabled) && Objects.equals(failedOnly, that.failedOnly) && Objects.equals(executable, that.executable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, failedOnly, executable);
    }

}
