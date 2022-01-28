package com.atlassian.bamboo.specs.api.builders.allure;

import com.atlassian.bamboo.specs.api.builders.plan.configuration.PluginConfiguration;
import com.atlassian.bamboo.specs.api.model.allure.AllureSpecsProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AllureSpecs extends PluginConfiguration<AllureSpecsProperties> {

    private Boolean enabled;
    private Boolean failedOnly;
    private String executable;
    private String artifactName;

    public AllureSpecs() {
    }

    public AllureSpecs(Boolean enabled, Boolean failedOnly, String executable, String artifactName) {
        this.enabled = enabled;
        this.failedOnly = failedOnly;
        this.executable = executable;
        this.artifactName = artifactName;
    }

    public void enabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void failedOnly(Boolean failedOnly) {
        this.failedOnly = failedOnly;
    }

    public void executable(String executable) {
        this.executable = executable;
    }

    public void artifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    @Override
    protected @NotNull AllureSpecsProperties build() {
        return new AllureSpecsProperties(this.enabled, this.failedOnly, this.executable, this.artifactName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllureSpecs that = (AllureSpecs) o;
        return Objects.equals(enabled, that.enabled) && Objects.equals(failedOnly, that.failedOnly) && Objects.equals(executable, that.executable) && Objects.equals(artifactName, that.artifactName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, failedOnly, executable, artifactName);
    }
}
