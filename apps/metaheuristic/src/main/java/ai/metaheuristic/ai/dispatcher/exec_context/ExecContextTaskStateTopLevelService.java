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

package ai.metaheuristic.ai.dispatcher.exec_context;

import ai.metaheuristic.ai.dispatcher.beans.TaskImpl;
import ai.metaheuristic.ai.dispatcher.event.TransferStateFromTaskQueueToExecContextEvent;
import ai.metaheuristic.ai.dispatcher.event.UpdateTaskExecStatesInGraphEvent;
import ai.metaheuristic.ai.dispatcher.repositories.TaskRepository;
import ai.metaheuristic.ai.dispatcher.task.TaskQueue;
import ai.metaheuristic.ai.dispatcher.task.TaskSyncService;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.task.TaskParamsYaml;
import ai.metaheuristic.commons.yaml.task.TaskParamsYamlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author Serge
 * Date: 12/18/2020
 * Time: 6:37 PM
 */
@Service
@Profile("dispatcher")
@Slf4j
@RequiredArgsConstructor
public class ExecContextTaskStateTopLevelService {

    private final ExecContextTaskStateService execContextTaskStateService;
    private final ExecContextSyncService execContextSyncService;
    private final TaskRepository taskRepository;
    private final TaskSyncService taskSyncService;

    @Async
    @EventListener
    public void updateTaskExecStatesInGraph(UpdateTaskExecStatesInGraphEvent event) {
        updateTaskExecStatesInGraph(event.execContextId, event.taskId);
    }

    @Async
    @EventListener
    public void transferStateFromTaskQueueToExecContext(TransferStateFromTaskQueueToExecContextEvent event) {
        transferStateFromTaskQueueToExecContext(event.execContextId);
    }


    public void transferStateFromTaskQueueToExecContext(Long execContextId) {
        for (int i = 0; i < 100; i++) {
            TaskQueue.TaskGroup taskGroup = execContextSyncService.getWithSync(execContextId,
                    () -> execContextTaskStateService.transferStateFromTaskQueueToExecContext(execContextId));

            if (taskGroup==null){
                return;
            }
            taskGroup.reset();
        }
    }

    public void updateTaskExecStatesInGraph(Long execContextId, Long taskId) {

        TaskImpl task = taskRepository.findById(taskId).orElse(null);
        if (task==null) {
            return;
        }
        TaskParamsYaml taskParams = TaskParamsYamlUtils.BASE_YAML_UTILS.to(task.getParams());
        if (!execContextId.equals(task.execContextId)) {
            log.error("(!execContextId.equals(task.execContextId))");
        }

        execContextSyncService.getWithSyncNullable(execContextId,
                () -> taskSyncService.getWithSyncNullable(taskId,
                        () -> execContextTaskStateService.updateTaskExecStatesInGraph(execContextId, taskId, EnumsApi.TaskExecState.from(task.execState), taskParams.task.taskContextId)));
    }
}