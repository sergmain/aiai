/*
 * Metaheuristic, Copyright (C) 2017-2019  Serge Maslyukov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ai.metaheuristic.ai.station.station_resource;

import ai.metaheuristic.ai.exceptions.ResourceProviderException;
import ai.metaheuristic.ai.resource.AssetFile;
import ai.metaheuristic.ai.resource.ResourceUtils;
import ai.metaheuristic.ai.station.env.EnvService;
import ai.metaheuristic.ai.station.LaunchpadLookupExtendedService;
import ai.metaheuristic.ai.station.StationTaskService;
import ai.metaheuristic.ai.yaml.env.DiskStorage;
import ai.metaheuristic.ai.yaml.env.EnvYaml;
import ai.metaheuristic.ai.yaml.metadata.Metadata;
import ai.metaheuristic.ai.yaml.station_task.StationTask;
import ai.metaheuristic.api.v1.EnumsApi;
import ai.metaheuristic.api.v1.data.SnippetApiData;
import ai.metaheuristic.api.v1.data_storage.DataStorageParams;
import ai.metaheuristic.api.v1.sourcing.DiskInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@Profile("station")
public class DiskResourceProvider implements ResourceProvider {

    private final EnvService envService;
    private final StationTaskService stationTaskService;

    public DiskResourceProvider(EnvService envService, StationTaskService stationTaskService) {
        this.envService = envService;
        this.stationTaskService = stationTaskService;
    }

    @Override
    public List<AssetFile> prepareForDownloadingDataFile(
            File taskDir, LaunchpadLookupExtendedService.LaunchpadLookupExtended launchpad,
            StationTask task, Metadata.LaunchpadInfo launchpadCode,
            String resourceCode, DataStorageParams dataStorageParams) {

        if (dataStorageParams.sourcing!= EnumsApi.DataSourcing.disk) {
            throw new ResourceProviderException("#015.018 Wrong type of sourcing of data storage" + dataStorageParams.sourcing);
        }
        DiskInfo diskInfo = dataStorageParams.disk;

        EnvYaml env = envService.getEnvYaml();
        DiskStorage diskStorage = env.findDiskStorageByCode(diskInfo.env);
        if (diskStorage==null) {
            throw new ResourceProviderException("#015.020 The disk storage wasn't found for code: " + diskInfo.env);
        }
        File path = new File(diskStorage.path);
        if (!path.exists()) {
            throw new ResourceProviderException("#015.024 The path of disk storage doesn't exist: " + path.getAbsolutePath());
        }

        AssetFile assetFile;
        if (diskInfo.code.equals("*")) {
            assetFile = new AssetFile();
            // TODO 2019.05.08 is this correct to declare file with '*' ?
            assetFile.setFile( new File(path, "*") );
            assetFile.setProvided(true);
            assetFile.setContent(true);
            assetFile.setError(false);
            assetFile.setExist(true);
            assetFile.setFileLength(0);
        }
        else {
            assetFile = ResourceUtils.prepareAssetFile(path, null, diskInfo.code);
        }

        return Collections.singletonList(assetFile);
    }

    @Override
    public SnippetApiData.SnippetExecResult processResultingFile(
            LaunchpadLookupExtendedService.LaunchpadLookupExtended launchpad,
            StationTask task, Metadata.LaunchpadInfo launchpadCode,
            File outputResourceFile
    ) {
        if (outputResourceFile.exists()) {
            log.info("The result data was already written to file {}, no need to upload to launchpad", outputResourceFile.getPath());
            stationTaskService.setResourceUploadedAndCompleted(launchpad.launchpadLookup.url, task.taskId);
        } else {
            String es = "#015.030 Result data file wasn't found, resultDataFile: " + outputResourceFile.getPath();
            log.error(es);
            return new SnippetApiData.SnippetExecResult(false, -1, es);
        }
        return null;    }

    @Override
    public File getOutputResourceFile(
            File taskDir, LaunchpadLookupExtendedService.LaunchpadLookupExtended launchpad,
            StationTask task, String outputResourceCode, DataStorageParams dataStorageParams) {

        EnvYaml env = envService.getEnvYaml();
        DiskStorage diskStorage = env.findDiskStorageByCode(dataStorageParams.disk.env);
        if (diskStorage==null) {
            throw new ResourceProviderException("#015.037 The disk storage wasn't found for code: " + dataStorageParams.disk.env);
        }
        File path = new File(diskStorage.path);
        if (!path.exists()) {
            throw new ResourceProviderException("#015.042 The path of disk storage doesn't exist: " + path.getAbsolutePath());
        }

        return new File(path, outputResourceCode);
    }

}