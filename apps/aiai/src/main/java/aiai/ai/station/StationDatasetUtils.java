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
package aiai.ai.station;

import aiai.ai.Consts;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class StationDatasetUtils {

    public static AssetFile prepareDatasetFile(File stationDatasetDir, long datasetId) {

        AssetFile datasetFile = new AssetFile();

        File currDir = new File(stationDatasetDir, String.format("%06d", datasetId));
        if (!currDir.exists()) {
            boolean isOk = currDir.mkdirs();
            if (!isOk) {
                log.error("Can't make all directories for path: {}", currDir);
                datasetFile.isError = true;
            }
        }

        datasetFile.file = new File(currDir, String.format(Consts.DATASET_FILE_MASK, datasetId));
        if (datasetFile.file.exists()) {
            if (datasetFile.file.length() == 0) {
                datasetFile.file.delete();
            }
            else {
                datasetFile.isContent = true;
            }
        }
        return datasetFile;
    }

    public static File checkAndCreateDatasetDir(File stationDir) {
        File dsDir = new File(stationDir, "dataset");
        if (!dsDir.exists()) {
            boolean isOk = dsDir.mkdirs();
            if (!isOk) {
                System.out.println("Can't make all directories for path: " + dsDir.getAbsolutePath());
                return null;
            }
        }
        return dsDir;
    }
}