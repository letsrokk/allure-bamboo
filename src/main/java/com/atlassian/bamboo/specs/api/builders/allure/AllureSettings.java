package com.atlassian.bamboo.specs.api.builders.allure;

import com.atlassian.bamboo.specs.api.builders.plan.configuration.PluginConfiguration;
import com.atlassian.bamboo.specs.api.model.allure.AllureSettingsProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class AllureSettings extends PluginConfiguration<AllureSettingsProperties> {

    private Boolean enabled;
    private Boolean failedOnly;
    private String executable;

    public AllureSettings() {
        this.enabled = FALSE;
        this.failedOnly = TRUE;
        this.executable = "allure-2.7.0";
    }

    public AllureSettings(Boolean enabled, Boolean failedOnly, String executable) {
        this.enabled = enabled;
        this.failedOnly = failedOnly;
        this.executable = executable;
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

    @Override
    protected @NotNull AllureSettingsProperties build() {
        return new AllureSettingsProperties(this.enabled, this.failedOnly, this.executable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllureSettings that = (AllureSettings) o;
        return Objects.equals(executable, that.executable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executable);
    }
}
