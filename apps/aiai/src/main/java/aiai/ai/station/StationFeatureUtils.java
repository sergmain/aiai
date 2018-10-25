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
import aiai.apps.commons.utils.DirUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class StationFeatureUtils {

    public static AssetFile prepareFeatureFile(File stationDir, long datasetId, long featureId) {

        final AssetFile assetFile = new AssetFile();

        final String featurePath = String.format("%06d%cfeature%c", datasetId, File.separatorChar, File.separatorChar);
        final File featureDir = DirUtils.createDir(stationDir, featurePath);
        assetFile.isExist = featureDir!=null && featureDir.exists();
        if (featureDir==null) {
            assetFile.isError = true;
            return assetFile;
        }

        assetFile.file = new File(featureDir, String.format(Consts.FEATURE_FILE_MASK, featureId));
        if (assetFile.file.exists()) {
            if (assetFile.file.length() == 0) {
                assetFile.file.delete();
            }
            else {
                assetFile.isContent = true;
            }
        }
        return assetFile;
    }
}