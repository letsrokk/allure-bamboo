/*
 *  Copyright 2016-2023 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.bamboo.util;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.move;

public final class ZipUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtil.class);

    private ZipUtil() {
        // do not instantiate
    }

    public static void unzip(final @NotNull Path zipFilePath,
                             final String outputDir) throws IOException {
        try (ZipFile zp = new ZipFile(zipFilePath.toFile())) {
            zp.extractAll(outputDir);
        } catch (ZipException e) {
            LOGGER.error("Unable to unpack archive {}: {}", zipFilePath.getFileName(), e.getMessage());
        }
    }

    public static void zipFolder(final @NotNull Path srcFolder,
                                 final @NotNull Path targetDir) throws IOException {
        final Path zipReportTmpDir = createTempDirectory("tmp_allure_report");
        final Path zipReport = zipReportTmpDir.resolve("report.zip");
        try (ZipFile zp = new ZipFile(zipReport.toFile())) {
            try {
                zp.addFolder(srcFolder.toFile());
            } catch (ZipException e) {
                LOGGER.error("Unable to zip folder {}: {}", srcFolder.getFileName(), e.getMessage());
            }
        }
        move(zipReport, targetDir, StandardCopyOption.REPLACE_EXISTING);
    }
}
