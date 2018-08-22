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

import aiai.ai.Globals;
import aiai.ai.beans.StationMetadata;
import aiai.ai.repositories.StationMetadataRepository;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Service
public class StationService {

    private final Globals globals;

    private final StationMetadataRepository stationMetadataRepository;

    public StationService(Globals globals, StationMetadataRepository stationMetadataRepository) {
        this.globals = globals;
        this.stationMetadataRepository = stationMetadataRepository;
    }

    public String getStationId() {
        return getMeta(StationConsts.STATION_ID);
    }

    public void setStationId(String stationId) {
        storeOrReplace(StationConsts.STATION_ID, stationId);
    }

    public String getEnv() {
        return getMeta(StationConsts.ENV);
    }

    public void setEnv(String stationId) {
        storeOrReplace(StationConsts.ENV, stationId);
    }

    @PostConstruct
    public void init() {
        final File file = new File(globals.stationDir, "env.yaml");
        if (!file.exists()) {
            System.out.println("Station's config file 'env.xml' doesn't exist.");
            return;
        }
        try {
            String env = FileUtils.readFileToString(file, Charsets.UTF_8);
            setEnv(env);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Error while loading file: " + file.getPath(), e);
        }

    }

    private synchronized void storeOrReplace(String key, String value) {
        StationMetadata metadata = stationMetadataRepository.findByKey(key).orElse(null);
        if (metadata != null) {
            metadata.setValue(value);
            stationMetadataRepository.save(metadata);
            return;
        }
        StationMetadata m = new StationMetadata();
        m.setKey(key);
        m.setValue(value);
        stationMetadataRepository.save(m);
    }


    private String getMeta(String key) {
        return stationMetadataRepository.findByKey(key).map(StationMetadata::getValue).orElse(null);
    }


}
