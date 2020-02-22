/*
 * Metaheuristic, Copyright (C) 2017-2020  Serge Maslyukov
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

package ai.metaheuristic.ai.station;

import ai.metaheuristic.ai.Enums;
import ai.metaheuristic.ai.yaml.communication.mh.dispatcher..DispatcherCommParamsYaml;
import ai.metaheuristic.ai.yaml.communication.station.StationCommParamsYaml;
import ai.metaheuristic.ai.yaml.metadata.FunctionDownloadStatusYaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Serge
 * Date: 8/29/2019
 * Time: 8:03 PM
 */
@Slf4j
@Service
@Profile("station")
@RequiredArgsConstructor
public class StationCommandProcessor {
    private final StationService stationService;
    private final MetadataService metadataService;
    private final CurrentExecState currentExecState;

    // this method is synchronized outside
    public void processLaunchpadCommParamsYaml(StationCommParamsYaml scpy, String mh.dispatcher.Url, DispatcherCommParamsYaml mh.dispatcher.Yaml) {
        scpy.resendTaskOutputResourceResult = resendTaskOutputResource(mh.dispatcher.Url, mh.dispatcher.Yaml);
        processExecContextStatus(mh.dispatcher.Url, mh.dispatcher.Yaml);
        processReportResultDelivering(mh.dispatcher.Url, mh.dispatcher.Yaml);
        processAssignedTask(mh.dispatcher.Url, mh.dispatcher.Yaml);
        storeStationId(mh.dispatcher.Url, mh.dispatcher.Yaml);
        reAssignStationId(mh.dispatcher.Url, mh.dispatcher.Yaml);
        registerFunctions(scpy.functionDownloadStatus, mh.dispatcher.Url, mh.dispatcher.Yaml);
    }

    private void registerFunctions(StationCommParamsYaml.FunctionDownloadStatus functionDownloadStatus, String mh.dispatcher.Url, DispatcherCommParamsYaml mh.dispatcher.Yaml) {
        List<FunctionDownloadStatusYaml.Status> statuses = metadataService.registerNewFunctionCode(mh.dispatcher.Url, mh.dispatcher.Yaml.functions.infos);
        for (FunctionDownloadStatusYaml.Status status : statuses) {
            functionDownloadStatus.statuses.add(new StationCommParamsYaml.FunctionDownloadStatus.Status(status.functionState, status.code));
        }
    }

    // processing at station side
    private StationCommParamsYaml.ResendTaskOutputResourceResult resendTaskOutputResource(String mh.dispatcher.Url, DispatcherCommParamsYaml request) {
        if (request.resendTaskOutputResource==null || request.resendTaskOutputResource.taskIds==null || request.resendTaskOutputResource.taskIds.isEmpty()) {
            return null;
        }
        List<StationCommParamsYaml.ResendTaskOutputResourceResult.SimpleStatus> statuses = new ArrayList<>();
        for (Long taskId : request.resendTaskOutputResource.taskIds) {
            Enums.ResendTaskOutputResourceStatus status = stationService.resendTaskOutputResource(mh.dispatcher.Url, taskId);
            statuses.add( new StationCommParamsYaml.ResendTaskOutputResourceResult.SimpleStatus(taskId, status));
        }
        return new StationCommParamsYaml.ResendTaskOutputResourceResult(statuses);
    }

    private void processExecContextStatus(String mh.dispatcher.Url, DispatcherCommParamsYaml request) {
        if (request.execContextStatus ==null) {
            return;
        }
        currentExecState.register(mh.dispatcher.Url, request.execContextStatus.statuses);
    }

    // processing at station side
    private void processReportResultDelivering(String mh.dispatcher.Url, DispatcherCommParamsYaml request) {
        if (request.reportResultDelivering==null) {
            return;
        }
        stationService.markAsDelivered(mh.dispatcher.Url, request.reportResultDelivering.getIds());
    }

    private void processAssignedTask(String mh.dispatcher.Url, DispatcherCommParamsYaml request) {
        if (request.assignedTask==null) {
            return;
        }
        stationService.assignTasks(mh.dispatcher.Url, request.assignedTask);
    }

    // processing at station side
    private void storeStationId(String mh.dispatcher.Url, DispatcherCommParamsYaml request) {
        if (request.assignedStationId==null) {
            return;
        }
        log.info("storeStationId() new station Id: {}", request.assignedStationId);
        metadataService.setStationIdAndSessionId(
                mh.dispatcher.Url, request.assignedStationId.assignedStationId, request.assignedStationId.assignedSessionId);
    }

    // processing at station side
    private void reAssignStationId(String mh.dispatcher.Url, DispatcherCommParamsYaml request) {
        if (request.reAssignedStationId==null) {
            return;
        }
        final String currStationId = metadataService.getStationId(mh.dispatcher.Url);
        final String currSessionId = metadataService.getSessionId(mh.dispatcher.Url);
        if (currStationId!=null && currSessionId!=null &&
                currStationId.equals(request.reAssignedStationId.getReAssignedStationId()) &&
                currSessionId.equals(request.reAssignedStationId.sessionId)
        ) {
            return;
        }

        log.info("reAssignStationId(),\n\t\tcurrent stationId: {}, sessionId: {}\n\t\t" +
                        "new stationId: {}, sessionId: {}",
                currStationId, currSessionId,
                request.reAssignedStationId.getReAssignedStationId(), request.reAssignedStationId.sessionId
        );
        metadataService.setStationIdAndSessionId(
                mh.dispatcher.Url, request.reAssignedStationId.getReAssignedStationId(), request.reAssignedStationId.sessionId);
    }

}
