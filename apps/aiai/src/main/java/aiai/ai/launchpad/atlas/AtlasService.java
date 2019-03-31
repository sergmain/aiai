/*
 * AiAi, Copyright (C) 2017-2019  Serge Maslyukov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package aiai.ai.launchpad.atlas;

import aiai.ai.Enums;
import aiai.ai.Globals;
import aiai.ai.launchpad.beans.*;
import aiai.ai.launchpad.binary_data.BinaryDataService;
import aiai.ai.launchpad.binary_data.SimpleCodeAndStorageUrl;
import aiai.ai.launchpad.data.AtlasData;
import aiai.ai.launchpad.data.BaseDataClass;
import aiai.ai.launchpad.data.OperationStatusRest;
import aiai.ai.launchpad.experiment.ExperimentCache;
import aiai.ai.launchpad.flow.FlowCache;
import aiai.ai.launchpad.repositories.*;
import aiai.ai.utils.ControllerUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Profile("launchpad")
public class AtlasService {

    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    private final Globals globals;
    private final BinaryDataService binaryDataService;
    private final FlowCache flowCache;
    private final FlowInstanceRepository flowInstanceRepository;
    private final ExperimentCache experimentCache;
    private final ExperimentFeatureRepository experimentFeatureRepository;
    private final ExperimentSnippetRepository experimentSnippetRepository;
    private final ExperimentTaskFeatureRepository experimentTaskFeatureRepository;
    private final TaskRepository taskRepository;
    private final ConsoleFormAtlasService consoleFormAtlasService;
    private final AtlasRepository atlasRepository;

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    public static class StoredToAtlasWithStatus extends BaseDataClass {
        public ExperimentStoredToAtlas experimentStoredToAtlas;
        public Enums.StoringStatus status;

        @SuppressWarnings("WeakerAccess")
        public StoredToAtlasWithStatus(Enums.StoringStatus status, String errorMessage) {
            this.status = status;
            this.errorMessages = Collections.singletonList(errorMessage);
        }
    }

    @Autowired
    public AtlasService(Globals globals, FlowCache flowCache, FlowInstanceRepository flowInstanceRepository, ExperimentCache experimentCache, ExperimentFeatureRepository experimentFeatureRepository, BinaryDataService binaryDataService, ExperimentSnippetRepository experimentSnippetRepository, ExperimentTaskFeatureRepository experimentTaskFeatureRepository, TaskRepository taskRepository, ConsoleFormAtlasService consoleFormAtlasService, AtlasRepository atlasRepository) {
        this.globals = globals;
        this.flowCache = flowCache;
        this.flowInstanceRepository = flowInstanceRepository;
        this.experimentCache = experimentCache;
        this.experimentFeatureRepository = experimentFeatureRepository;
        this.binaryDataService = binaryDataService;
        this.experimentSnippetRepository = experimentSnippetRepository;
        this.experimentTaskFeatureRepository = experimentTaskFeatureRepository;
        this.taskRepository = taskRepository;
        this.consoleFormAtlasService = consoleFormAtlasService;
        this.atlasRepository = atlasRepository;
    }

    public AtlasData.AtlasSimpleExperiments getAtlasExperiments(Pageable pageable) {
        pageable = ControllerUtils.fixPageSize(globals.atlasExperimentRowsLimit, pageable);
        AtlasData.AtlasSimpleExperiments result = new AtlasData.AtlasSimpleExperiments();
        result.items = atlasRepository.findAllAsSimple(pageable);
        return result;
    }

    public OperationStatusRest toAtlas(long flowInstanceId, long experimentId) {
        StoredToAtlasWithStatus stored = toExperimentStoredToAtlas(experimentId);
        if (stored.isErrorMessages()) {
            return new OperationStatusRest(Enums.OperationStatus.ERROR, stored.errorMessages);
        }
        if (flowInstanceId!=stored.experimentStoredToAtlas.flowInstance.id) {
            return new OperationStatusRest(Enums.OperationStatus.ERROR, "Experiment can't be stored, flowInstanceId is different");
        }
        String poolCode = getPoolCodeForExperiment(flowInstanceId, experimentId);
        List<SimpleCodeAndStorageUrl> codes = binaryDataService.getResourceCodesInPool(List.of(poolCode));
        if (!codes.isEmpty()) {
            return new OperationStatusRest(Enums.OperationStatus.ERROR, "Experiment already stored");
        }
        Atlas b = new Atlas();
        try {
            b.experiment = toJson(stored.experimentStoredToAtlas);
        } catch (JsonProcessingException e) {
            return new OperationStatusRest(Enums.OperationStatus.ERROR,
                    "General error while storing experiment, " + e.toString());
        }
        b.name = stored.experimentStoredToAtlas.experiment.getName();
        b.description = stored.experimentStoredToAtlas.experiment.getDescription();
        b.code = stored.experimentStoredToAtlas.experiment.getCode();
        b.createdOn = stored.experimentStoredToAtlas.experiment.getCreatedOn();
        atlasRepository.save(b);

        ConsoleOutputStoredToAtlas filed = toConsoleOutputStoredToAtlas(
                stored.experimentStoredToAtlas.flowInstance.id);
        if (filed.isErrorMessages()) {
            return new OperationStatusRest(Enums.OperationStatus.ERROR, filed.errorMessages);
        }
        try(InputStream is = new FileInputStream(filed.dumpOfConsoleOutputs)) {
//            public BinaryData save(InputStream is, long size,
//            Enums.BinaryDataType binaryDataType, String code, String poolCode,
//            boolean isManual, String filename, Long flowInstanceId) {
            //noinspection unused
            BinaryData data = binaryDataService.save(
                    is, filed.dumpOfConsoleOutputs.length(), Enums.BinaryDataType.CONSOLE,
                    poolCode, poolCode, false, null, null);

        } catch (FileNotFoundException e) {
            return new OperationStatusRest(Enums.OperationStatus.ERROR,
                    "A problem with stored console outputs, try to run again");
        } catch (RuntimeException | IOException e) {
            log.error("Error", e);
            return new OperationStatusRest(Enums.OperationStatus.ERROR,
                    "Error storing console outputs to db, " + e.toString());
        }

        return OperationStatusRest.OPERATION_STATUS_OK;
    }

    @SuppressWarnings("WeakerAccess")
    public static String getPoolCodeForExperiment(long flowInstanceId, long experimentId) {
        return String.format("stored-experiment-%d-%d",flowInstanceId, experimentId);
    }

    public StoredToAtlasWithStatus toExperimentStoredToAtlas(long experimentId) {

        Experiment experiment = experimentCache.findById(experimentId);
        if (experiment==null) {
            return new StoredToAtlasWithStatus(Enums.StoringStatus.CANT_BE_STORED,
                    "#604.02 can't find experiment for id: " + experimentId);
        }
        FlowInstance flowInstance = flowInstanceRepository.findById(experiment.flowInstanceId).orElse(null);
        if (flowInstance==null) {
            return new StoredToAtlasWithStatus(Enums.StoringStatus.CANT_BE_STORED,
                    "#604.05 can't find flowInstance for this experiment");
        }
        Flow flow = flowCache.findById(flowInstance.flowId);
        if (flow==null) {
            return new StoredToAtlasWithStatus(Enums.StoringStatus.CANT_BE_STORED,
                    "#604.10 can't find flow for this experiment");
        }
        StoredToAtlasWithStatus result = new StoredToAtlasWithStatus();

        List<ExperimentFeature> features = experimentFeatureRepository.findByExperimentId(experimentId);
        List<ExperimentSnippet> snippets = experimentSnippetRepository.findByExperimentId(experimentId);
        List<ExperimentTaskFeature> taskFeatures = experimentTaskFeatureRepository.findByFlowInstanceId(flowInstance.id);
        List<Task> tasks = taskRepository.findAllByFlowInstanceId(flowInstance.id);

        result.experimentStoredToAtlas = new ExperimentStoredToAtlas(
                flow, flowInstance, experiment,
                features, experiment.hyperParams, snippets, taskFeatures, tasks
        );
        result.status = Enums.StoringStatus.OK;
        return result;
    }

    public ExperimentStoredToAtlas fromJson(String json) throws IOException {
        //noinspection UnnecessaryLocalVariable
        ExperimentStoredToAtlas estb1 = mapper.readValue(json, ExperimentStoredToAtlas.class);
        return estb1;
    }


    public String toJson(ExperimentStoredToAtlas stored) throws JsonProcessingException {
        //noinspection UnnecessaryLocalVariable
        String json = mapper.writeValueAsString(stored);
        return json;
    }

    @SuppressWarnings("WeakerAccess")
    public ConsoleOutputStoredToAtlas toConsoleOutputStoredToAtlas(long flowInstanceId) {
        //noinspection UnnecessaryLocalVariable
        ConsoleOutputStoredToAtlas result = consoleFormAtlasService.collectConsoleOutputs(flowInstanceId);
        return result;
    }
}
