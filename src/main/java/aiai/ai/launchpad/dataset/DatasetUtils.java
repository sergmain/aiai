/*
 AiAi, Copyright (C) 2017 - 2018, Serge Maslyukov

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */
package aiai.ai.launchpad.dataset;

import aiai.ai.Consts;
import aiai.ai.utils.DirUtils;

import java.io.File;

public class DatasetUtils {

    public static File getDatasetFile(File launchpadDir, long datasetId) {
        final String definitionPath = String.format("%s%c%03d", Consts.DEFINITIONS_DIR, File.separatorChar, datasetId);

        final File definitionDir = DirUtils.createDir(launchpadDir, definitionPath);
        if (definitionDir==null || !definitionDir.exists()) {
            throw new IllegalStateException("Error create directory: " + definitionPath);
        }

        final String datasetPath = String.format("%s%cdataset%c%s", definitionPath, File.separatorChar, File.separatorChar, Consts.DATASET_TXT);
        final File datasetFile = new File(launchpadDir, datasetPath);
        if (!datasetFile.exists()) {
            throw new IllegalStateException("Dataset file doesn't exist: " + datasetFile.getAbsolutePath());
        }
        return datasetFile;
    }

    public static File getFeatureFile(File launchpadDir, long datasetId, long featureId) {
        final String definitionPath = String.format("%s%c%03d", Consts.DEFINITIONS_DIR, File.separatorChar, datasetId);

        final File definitionDir = DirUtils.createDir(launchpadDir, definitionPath);
        if (definitionDir==null || !definitionDir.exists()) {
            throw new IllegalStateException("Error create directory: " + definitionPath);
        }

        final String featureNumner = String.format("%03d", featureId);
        final File featureDir = DirUtils.createDir(definitionDir, featureNumner);
        if (featureDir==null || !featureDir.exists()) {
            throw new IllegalStateException("Error create feature dir: " + featureNumner);
        }

        final String featureFilename = String.format(Consts.FEATURE_FILE_MASK, featureId);
        final File datasetFile = new File(featureDir, featureFilename);
        if (!datasetFile.exists()) {
            throw new IllegalStateException("Dataset file doesn't exist: " + datasetFile.getAbsolutePath());
        }
        return datasetFile;
    }
}
