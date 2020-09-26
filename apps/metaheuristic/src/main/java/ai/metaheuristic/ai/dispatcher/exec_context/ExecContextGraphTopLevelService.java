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

import ai.metaheuristic.ai.dispatcher.beans.ExecContextImpl;
import ai.metaheuristic.ai.dispatcher.data.ExecContextData;
import ai.metaheuristic.ai.dispatcher.task.TaskExecStateService;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.OperationStatusRest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author Serge
 * Date: 1/24/2020
 * Time: 1:02 AM
 */
@Service
@Profile("dispatcher")
@Slf4j
@RequiredArgsConstructor
public class ExecContextGraphTopLevelService {

    private final ExecContextGraphService execContextGraphService;
    private final ExecContextSyncService execContextSyncService;
    private final TaskExecStateService taskExecStateService;
    private final ExecContextCache execContextCache;

    // section 'execContext graph methods'

    // read-only operations with graph
    public List<ExecContextData.TaskVertex> findAll(ExecContextImpl execContext) {
        List<ExecContextData.TaskVertex> vertexList = execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findAll(execContext));
        return vertexList;
    }

    public List<ExecContextData.TaskVertex> findLeafs(ExecContextImpl execContext) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findLeafs(execContext));
    }

    public Set<ExecContextData.TaskVertex> findDescendants(ExecContextImpl execContext, Long taskId) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findDescendants(execContext, taskId));
    }

    public Set<ExecContextData.TaskVertex> findDirectDescendants(ExecContextImpl execContext, Long taskId) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findDirectDescendants(execContext, taskId));
    }

    public Set<ExecContextData.TaskVertex> findDirectAncestors(ExecContextImpl execContext, ExecContextData.TaskVertex vertex) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findDirectAncestors(execContext, vertex));
    }

    public List<ExecContextData.TaskVertex> findAllForAssigning(ExecContextImpl execContext) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findAllForAssigning(execContext));
    }

    public List<ExecContextData.TaskVertex> findAllBroken(ExecContextImpl execContext) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.findAllBroken(execContext));
    }

    public Long getCountUnfinishedTasks(ExecContextImpl execContext) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.getCountUnfinishedTasks(execContext));
    }

    public List<ExecContextData.TaskVertex> getUnfinishedTaskVertices(ExecContextImpl execContext) {
        return execContextSyncService.getWithSyncReadOnly(execContext, () -> execContextGraphService.getUnfinishedTaskVertices(execContext));
    }

}
