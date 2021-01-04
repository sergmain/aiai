/*
 * Metaheuristic, Copyright (C) 2017-2020, Innovation platforms, LLC
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

package ai.metaheuristic.ai.processor;

import ai.metaheuristic.ai.processor.data.ProcessorData;
import ai.metaheuristic.ai.yaml.communication.keep_alive.KeepAliveRequestParamYaml;
import ai.metaheuristic.ai.yaml.communication.keep_alive.KeepAliveResponseParamYaml;
import ai.metaheuristic.ai.yaml.metadata.MetadataParamsYaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

import static ai.metaheuristic.ai.processor.ProcessorAndCoreData.*;

/**
 * @author Serge
 * Date: 11/21/2020
 * Time: 11:17 AM
 */
@Slf4j
@Service
@Profile("processor")
@RequiredArgsConstructor
public class ProcessorKeepAliveProcessor {
    private final ProcessorService processorService;
    private final MetadataService metadataService;
    private final CurrentExecState currentExecState;

    public void processKeepAliveResponseParamYaml(KeepAliveRequestParamYaml karpy, DispatcherUrl dispatcherUrl, KeepAliveResponseParamYaml responseParamYaml) {
        processExecContextStatus(dispatcherUrl, responseParamYaml.execContextStatus);
        storeProcessorId(dispatcherUrl, responseParamYaml);
        reAssignProcessorId(dispatcherUrl, responseParamYaml);
        registerFunctions(karpy.functions, dispatcherUrl, responseParamYaml);
//        processRequestLogFile(pcpy)
    }

    private void registerFunctions(KeepAliveRequestParamYaml.FunctionDownloadStatuses functionDownloadStatus, DispatcherUrl dispatcherUrl, KeepAliveResponseParamYaml dispatcherYaml) {

        List<MetadataParamsYaml.Status> statuses = metadataService.registerNewFunctionCode(dispatcherUrl, dispatcherYaml.functions.infos);
        for (MetadataParamsYaml.Status status : statuses) {
            functionDownloadStatus.statuses.add(new KeepAliveRequestParamYaml.FunctionDownloadStatuses.Status(status.code, status.functionState));
        }
    }

    private void processExecContextStatus(DispatcherUrl dispatcherUrl, KeepAliveResponseParamYaml.ExecContextStatus execContextStatus) {
        currentExecState.register(dispatcherUrl, execContextStatus.statuses);
    }

    // processing at processor side
    private void storeProcessorId(ProcessorData.ProcessorCodeAndIdAndDispatcherUrlRef ref, KeepAliveResponseParamYaml request) {
        if (request.assignedProcessorId ==null) {
            return;
        }
        log.info("storeProcessorId() new processor Id: {}", request.assignedProcessorId);
        metadataService.setProcessorIdAndSessionId(
                ref, request.assignedProcessorId.assignedProcessorId.toString(), request.assignedProcessorId.assignedSessionId);
    }

    // processing at processor side
    private void reAssignProcessorId(ProcessorData.ProcessorCodeAndIdAndDispatcherUrlRef ref, KeepAliveResponseParamYaml response) {
        if (response.reAssignedProcessorId ==null) {
            return;
        }
        final String currProcessorId = metadataService.getProcessorId(ref);
        final String currSessionId = metadataService.getSessionId(ref);
        if (currProcessorId!=null && currSessionId!=null &&
                currProcessorId.equals(response.reAssignedProcessorId.getReAssignedProcessorId()) &&
                currSessionId.equals(response.reAssignedProcessorId.sessionId)
        ) {
            return;
        }

        log.info("reAssignProcessorId(),\n\t\tcurrent processorId: {}, sessionId: {}\n\t\t" +
                        "new processorId: {}, sessionId: {}",
                currProcessorId, currSessionId,
                response.reAssignedProcessorId.getReAssignedProcessorId(), response.reAssignedProcessorId.sessionId
        );
        metadataService.setProcessorIdAndSessionId(
                ref, response.reAssignedProcessorId.getReAssignedProcessorId(), response.reAssignedProcessorId.sessionId);
    }

}
