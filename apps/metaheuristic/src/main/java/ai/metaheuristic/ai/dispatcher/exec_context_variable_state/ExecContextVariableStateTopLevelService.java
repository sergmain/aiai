/*
 * Metaheuristic, Copyright (C) 2017-2021, Innovation platforms, LLC
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

package ai.metaheuristic.ai.dispatcher.exec_context_variable_state;

import ai.metaheuristic.ai.dispatcher.beans.ExecContextImpl;
import ai.metaheuristic.ai.dispatcher.event.TaskCreatedEvent;
import ai.metaheuristic.ai.dispatcher.event.VariableUploadedEvent;
import ai.metaheuristic.ai.dispatcher.exec_context.ExecContextCache;
import ai.metaheuristic.ai.dispatcher.exec_context.ExecContextSyncService;
import ai.metaheuristic.api.data.exec_context.ExecContextApiData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Serge
 * Date: 3/20/2021
 * Time: 1:03 AM
 */
@Service
@Profile("dispatcher")
@Slf4j
@RequiredArgsConstructor
public class ExecContextVariableStateTopLevelService {

    public final ExecContextVariableStateSyncService execContextVariableStateSyncService;
    public final ExecContextSyncService execContextSyncService;
    public final ExecContextVariableStateService execContextVariableStateService;
    public final ExecContextCache execContextCache;

    private static Map<Long, List<ExecContextApiData.VariableState>> taskCreatedEvents = new HashMap<>();
    private static Map<Long, List<VariableUploadedEvent>> variableUploadedEvents = new HashMap<>();

    public void registerCreatedTask(TaskCreatedEvent event) {
        taskCreatedEvents.computeIfAbsent(event.taskVariablesInfo.execContextId, k->new ArrayList<>()).add(event.taskVariablesInfo);
    }

    public void registerVariableState(VariableUploadedEvent event) {
        variableUploadedEvents.computeIfAbsent(event.execContextId, k->new ArrayList<>()).add(event);
    }

    public void processFlushing() {
        if (taskCreatedEvents.isEmpty() && variableUploadedEvents.isEmpty()) {
            return;
        }

        Map<Long, List<ExecContextApiData.VariableState>> taskCreatedEventsTemp = taskCreatedEvents;
        taskCreatedEvents = new HashMap<>();
        processCreatedTasks(taskCreatedEventsTemp);

        Map<Long, List<VariableUploadedEvent>> variableUploadedEventsTemp = variableUploadedEvents;
        variableUploadedEvents = new HashMap<>();
        processVariableStates(variableUploadedEventsTemp);
    }

    private void processCreatedTasks(Map<Long, List<ExecContextApiData.VariableState>> taskCreatedEvents) {
        for (Map.Entry<Long, List<ExecContextApiData.VariableState>> entry : taskCreatedEvents.entrySet()) {
            Long execContextVariableStateId = getExecContextVariableStateId(entry.getKey());
            if (execContextVariableStateId == null) {
                return;
            }
            execContextVariableStateSyncService.getWithSyncNullable(execContextVariableStateId,
                    () -> execContextVariableStateService.registerCreatedTasks(execContextVariableStateId, entry.getValue()));
        }
    }

    private void processVariableStates(Map<Long, List<VariableUploadedEvent>> events) {
        for (Map.Entry<Long, List<VariableUploadedEvent>> entry : events.entrySet()) {
            Long execContextVariableStateId = getExecContextVariableStateId(entry.getKey());
            if (execContextVariableStateId == null) {
                return;
            }
            execContextVariableStateSyncService.getWithSyncNullable(execContextVariableStateId,
                    () -> registerVariableStateInternal(entry.getKey(), execContextVariableStateId, entry.getValue()));
        }
    }

    @Nullable
    private Long getExecContextVariableStateId(Long execContextId) {
        ExecContextImpl execContext = execContextCache.findById(execContextId);
        if (execContext==null) {
            return null;
        }
        return execContext.execContextVariableStateId;
    }

    // this method is here to work around some strange situation
    // about calling transactional method from lambda
    private Void registerVariableStateInternal(Long execContextId, Long execContextVariableStateId, List<VariableUploadedEvent> event) {
        return execContextVariableStateService.registerVariableStates(execContextId, execContextVariableStateId, event);
    }


}
