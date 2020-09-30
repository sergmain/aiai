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

import ai.metaheuristic.ai.Enums;
import ai.metaheuristic.ai.dispatcher.beans.ExecContextImpl;
import ai.metaheuristic.ai.dispatcher.beans.Processor;
import ai.metaheuristic.ai.dispatcher.beans.TaskImpl;
import ai.metaheuristic.ai.dispatcher.data.ExecContextData;
import ai.metaheuristic.ai.dispatcher.data.InternalFunctionData;
import ai.metaheuristic.ai.dispatcher.event.DispatcherEventService;
import ai.metaheuristic.ai.dispatcher.event.TaskWithInternalContextEvent;
import ai.metaheuristic.ai.dispatcher.repositories.TaskRepository;
import ai.metaheuristic.ai.dispatcher.task.TaskExecStateService;
import ai.metaheuristic.ai.dispatcher.task.TaskHelperService;
import ai.metaheuristic.ai.dispatcher.task.TaskTransactionalService;
import ai.metaheuristic.ai.utils.TxUtils;
import ai.metaheuristic.ai.yaml.communication.processor.ProcessorCommParamsYaml;
import ai.metaheuristic.ai.yaml.exec_context.ExecContextParamsYamlUtils;
import ai.metaheuristic.ai.yaml.function_exec.FunctionExecUtils;
import ai.metaheuristic.ai.yaml.processor_status.ProcessorStatusYaml;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.FunctionApiData;
import ai.metaheuristic.api.data.OperationStatusRest;
import ai.metaheuristic.api.data.exec_context.ExecContextParamsYaml;
import ai.metaheuristic.api.data.task.TaskApiData;
import ai.metaheuristic.api.data.task.TaskParamsYaml;
import ai.metaheuristic.api.dispatcher.ExecContext;
import ai.metaheuristic.api.dispatcher.Task;
import ai.metaheuristic.commons.S;
import ai.metaheuristic.commons.exceptions.DowngradeNotSupportedException;
import ai.metaheuristic.commons.utils.FunctionCoreUtils;
import ai.metaheuristic.commons.yaml.task.TaskParamsYamlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Serge
 * Date: 1/18/2020
 * Time: 3:34 PM
 */
@Service
@Profile("dispatcher")
@Slf4j
@RequiredArgsConstructor
public class ExecContextFSM {

    private final ExecContextCache execContextCache;
    private final ExecContextGraphService execContextGraphService;
    private final ExecContextGraphTopLevelService execContextGraphTopLevelService;
    private final ExecContextSyncService execContextSyncService;
    private final TaskExecStateService taskExecStateService;
    private final TaskRepository taskRepository;
    private final TaskHelperService taskHelperService;
    private final TaskTransactionalService taskTransactionalService;
    private final DispatcherEventService dispatcherEventService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void toStarted(ExecContext execContext) {
        toStarted(execContext.getId());
    }

    @Transactional
    public void toStarted(Long execContextId) {
        toState(execContextId, EnumsApi.ExecContextState.STARTED);
    }

    @Transactional
    public void toStopped(Long execContextId) {
        toState(execContextId, EnumsApi.ExecContextState.STOPPED);
    }

    @Transactional
    public void toProduced(Long execContextId) {
        toState(execContextId, EnumsApi.ExecContextState.PRODUCED);
    }

    @Transactional
    public void toFinished(Long execContextId) {
        toStateWithCompletion(execContextId, EnumsApi.ExecContextState.FINISHED);
    }

    @Transactional
    public void toError(Long execContextId) {
        toStateWithCompletion(execContextId, EnumsApi.ExecContextState.ERROR);
    }

    @Transactional
    public void toState(Long execContextId, EnumsApi.ExecContextState state) {
        ExecContextImpl execContext = execContextCache.findById(execContextId);
        if (execContext==null) {
            return;
        }
        if (execContext.state !=state.code) {
            execContext.setState(state.code);
            execContextCache.save(execContext);
        }
    }

    @Transactional
    public OperationStatusRest addTasksToGraph(@Nullable ExecContextImpl execContext, List<Long> parentTaskIds, List<TaskApiData.TaskWithContext> taskIds) {
        OperationStatusRest osr = execContextGraphService.addNewTasksToGraph(execContext, parentTaskIds, taskIds);
        return osr;
    }

    private void toStateWithCompletion(Long execContextId, EnumsApi.ExecContextState state) {
        ExecContextImpl execContext = execContextCache.findById(execContextId);
        if (execContext==null) {
            return;
        }
        if (execContext.state != state.code) {
            execContext.setCompletedOn(System.currentTimeMillis());
            execContext.setState(state.code);
            execContextCache.save(execContext);
        } else if (execContext.state!= EnumsApi.ExecContextState.FINISHED.code && execContext.completedOn != null) {
            log.error("Integrity failed, current state: {}, new state: {}, but execContext.completedOn!=null",
                    execContext.state, state.code);
        }
    }

    // methods related to Tasks

    @Transactional
    public void storeExecResult(ProcessorCommParamsYaml.ReportTaskProcessingResult.SimpleTaskExecResult result) {
        TaskImpl task = taskRepository.findById(result.taskId).orElse(null);
        if (task==null) {
            log.warn("Reporting about non-existed task #{}", result.taskId);
            return;
        }
        execContextSyncService.getWithSyncNullable(task.execContextId, () -> {
            storeExecResultInternal(result);
            return null;
        });
    }

    @Transactional
    public OperationStatusRest resetTask(Long execContextId, Long taskId) {
        Task t = taskExecStateService.resetTask(taskId);
        if (t == null) {
            String es = S.f("#705.0950 Found a non-existed task, graph consistency for execContextId #%s is failed",
                    execContextId);
            log.error(es);
            toError(execContextId);
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, es);
        }

        updateTaskExecStates(execContextCache.findById(execContextId), taskId, EnumsApi.TaskExecState.NONE.value, null);
        return OperationStatusRest.OPERATION_STATUS_OK;
    }

    public void markAsFinishedWithError(TaskImpl task, ExecContextImpl execContext, TaskParamsYaml taskParamsYaml, InternalFunctionData.InternalFunctionProcessingResult result) {
        log.error("#707.050 error type: {}, message: {}\n\tsourceCodeId: {}, execContextId: {}", result.processing, result.error, execContext.sourceCodeId, execContext.id);
        final Long taskId = task.id;
        final String console = "#707.060 Task #" + taskId + " was finished with status '" + result.processing + "', text of error: " + result.error;

        final Long execContextId = task.execContextId;
        final String taskContextId = taskParamsYaml.task.taskContextId;

        finishWithError(taskId, console, execContextId, taskContextId);
    }

    @Transactional
    public void finishWithError(Long taskId, Long execContextId, @Nullable String taskContextId, @Nullable String params) {
        finishWithError(taskId, "#303.080 Task was finished with an unknown error, can't process it", execContextId, taskContextId);
    }

    public void finishWithError(Long taskId, String console, Long execContextId, @Nullable String taskContextId) {
        execContextSyncService.getWithSyncNullable(execContextId, () -> {
            taskExecStateService.finishTaskAsError(taskId, EnumsApi.TaskExecState.ERROR, -10001, console);

            final ExecContextOperationStatusWithTaskList status = execContextGraphService.updateTaskExecState(
                    execContextCache.findById(execContextId), taskId, EnumsApi.TaskExecState.ERROR.value, taskContextId);
            taskExecStateService.updateTasksStateInDb(status);
            return null;
        });
    }

    // write operations with graph
    @Transactional
    public OperationStatusRest updateTaskExecStates(@Nullable ExecContextImpl execContext, Long taskId, int execState, @Nullable String taskContextId) {
        if (execContext==null) {
            // this execContext was deleted
            return OperationStatusRest.OPERATION_STATUS_OK;
        }
        TxUtils.checkTx();
        execContextSyncService.checkWriteLockPresent(execContext.id);

        taskExecStateService.changeTaskState(taskId, EnumsApi.TaskExecState.from(execState));
        final ExecContextOperationStatusWithTaskList status = execContextGraphService.updateTaskExecState(execContext, taskId, execState, taskContextId);
        taskExecStateService.updateTasksStateInDb(status);
        return status.status;
    }

    @Transactional
    public void processResendTaskOutputResourceResult(@Nullable String processorId, Enums.ResendTaskOutputResourceStatus status, Long taskId, Long variableId) {
        TaskImpl task = taskRepository.findById(taskId).orElse(null);
        if (task==null) {
            log.warn("#317.020 Task obsolete and was already deleted");
            return;
        }

        execContextSyncService.getWithSyncNullable(task.execContextId, () -> {
            switch (status) {
                case SEND_SCHEDULED:
                    log.info("#317.010 Processor #{} scheduled sending of output variables of task #{} for sending. This is normal operation of Processor", processorId, taskId);
                    break;
                case VARIABLE_NOT_FOUND:
                case TASK_IS_BROKEN:
                case TASK_PARAM_FILE_NOT_FOUND:
                    TaskParamsYaml tpy = TaskParamsYamlUtils.BASE_YAML_UTILS.to(task.params);
                    finishWithError(taskId, task.execContextId, tpy.task.taskContextId, task.params);
                    break;
                case OUTPUT_RESOURCE_ON_EXTERNAL_STORAGE:
                    Enums.UploadResourceStatus uploadResourceStatus = taskTransactionalService.setResultReceived(task, variableId);
                    if (uploadResourceStatus == Enums.UploadResourceStatus.OK) {
                        log.info("#317.040 the output resource of task #{} is stored on external storage which was defined by disk://. This is normal operation of sourceCode", taskId);
                    } else {
                        log.info("#317.050 can't update isCompleted field for task #{}", taskId);
                    }
                    break;
            }
            return null;
        });
    }

    private final Map<Long, AtomicLong> bannedSince = new HashMap<>();

    private List<TaskImpl> getAllByProcessorIdIsNullAndExecContextIdAndIdIn(Long execContextId, List<ExecContextData.TaskVertex> vertices, int page) {
        final List<Long> idsForSearch = getIdsForSearch(vertices, page, 20);
        if (idsForSearch.isEmpty()) {
            return List.of();
        }
        return taskRepository.findForAssigning(execContextId, idsForSearch);
    }

    @Nullable
    @Transactional
    public TaskImpl findUnassignedTaskAndAssign(Long execContextId, Processor processor, ProcessorStatusYaml psy, boolean isAcceptOnlySigned) {
        execContextSyncService.checkWriteLockPresent(execContextId);

        AtomicLong longHolder = bannedSince.computeIfAbsent(processor.getId(), o -> new AtomicLong(0));
        if (longHolder.get()!=0 && System.currentTimeMillis() - longHolder.get() < TimeUnit.MINUTES.toMillis(30)) {
            return null;
        }
        ExecContextImpl execContext = execContextCache.findById(execContextId);
        if (execContext==null) {
            return null;
        }
        final List<ExecContextData.TaskVertex> vertices = execContextGraphTopLevelService.findAllForAssigning(execContext);
        if (vertices.isEmpty()) {
            return null;
        }

        int page = 0;
        TaskImpl resultTask = null;
        List<TaskImpl> tasks;
        while ((tasks = getAllByProcessorIdIsNullAndExecContextIdAndIdIn(execContextId, vertices, page++)).size()>0) {
            for (TaskImpl task : tasks) {
                final TaskParamsYaml taskParamYaml;
                try {
                    taskParamYaml = TaskParamsYamlUtils.BASE_YAML_UTILS.to(task.getParams());
                }
                catch (YAMLException e) {
                    log.error("#705.420 Task #{} has broken params yaml and will be skipped, error: {}, params:\n{}", task.getId(), e.toString(),task.getParams());
                    finishWithError(task.getId(), task.execContextId, null, null);
                    continue;
                }
                if (task.execState==EnumsApi.TaskExecState.IN_PROGRESS.value) {
                    // may be happened because of multi-threaded processing of internal function
                    continue;
                }
                if (task.execState!=EnumsApi.TaskExecState.NONE.value) {
                    log.warn("#705.440 Task #{} with function '{}' was already processed with status {}",
                            task.getId(), taskParamYaml.task.function.code, EnumsApi.TaskExecState.from(task.execState));
                    continue;
                }
                // all tasks with internal function will be processed in a different thread
                if (taskParamYaml.task.context== EnumsApi.FunctionExecContext.internal) {
                    // Do Not set EnumsApi.TaskExecState.IN_PROGRESS here.
                    // it'll be set in ai.metaheuristic.ai.dispatcher.event.TaskWithInternalContextEventService.handleAsync
                    task.setAssignedOn(System.currentTimeMillis());
                    task.setResultResourceScheduledOn(0);
                    task = taskTransactionalService.save(task);
                    prepareVariables(task);

                    eventPublisher.publishEvent(new TaskWithInternalContextEvent(task.execContextId, task.id));
                    continue;
                }

                if (gitUnavailable(taskParamYaml.task, psy.gitStatusInfo.status!= Enums.GitStatus.installed) ) {
                    log.warn("#705.460 Can't assign task #{} to processor #{} because this processor doesn't correctly installed git, git status info: {}",
                            processor.getId(), task.getId(), psy.gitStatusInfo
                    );
                    longHolder.set(System.currentTimeMillis());
                    continue;
                }

                if (!S.b(taskParamYaml.task.function.env)) {
                    String interpreter = psy.env.getEnvs().get(taskParamYaml.task.function.env);
                    if (interpreter == null) {
                        log.warn("#705.480 Can't assign task #{} to processor #{} because this processor doesn't have defined interpreter for function's env {}",
                                task.getId(), processor.getId(), taskParamYaml.task.function.env
                        );
                        longHolder.set(System.currentTimeMillis());
                        continue;
                    }
                }

                final List<EnumsApi.OS> supportedOS = FunctionCoreUtils.getSupportedOS(taskParamYaml.task.function.metas);
                if (psy.os!=null && !supportedOS.isEmpty() && !supportedOS.contains(psy.os)) {
                    log.info("#705.500 Can't assign task #{} to processor #{}, " +
                                    "because this processor doesn't support required OS version. processor: {}, function: {}",
                            processor.getId(), task.getId(), psy.os, supportedOS
                    );
                    longHolder.set(System.currentTimeMillis());
                    continue;
                }

                if (isAcceptOnlySigned) {
                    if (taskParamYaml.task.function.checksumMap==null || taskParamYaml.task.function.checksumMap.keySet().stream().noneMatch(o->o.isSigned)) {
                        log.warn("#705.520 Function with code {} wasn't signed", taskParamYaml.task.function.getCode());
                        continue;
                    }
                }
                resultTask = task;
                // check that downgrading is supported
                try {
                    TaskParamsYaml tpy = TaskParamsYamlUtils.BASE_YAML_UTILS.to(task.getParams());
                    String params = TaskParamsYamlUtils.BASE_YAML_UTILS.toStringAsVersion(tpy, psy.taskParamsVersion);
                } catch (DowngradeNotSupportedException e) {
                    log.warn("#705.540 Task #{} can't be assigned to processor #{} because it's too old, downgrade to required taskParams level {} isn't supported",
                            resultTask.getId(), processor.id, psy.taskParamsVersion);
                    longHolder.set(System.currentTimeMillis());
                    resultTask = null;
                }
                if (resultTask!=null) {
                    break;
                }
            }
            if (resultTask!=null) {
                break;
            }
        }
        if (resultTask==null) {
            return null;
        }

        // normal way of operation
        longHolder.set(0);

/*
        taskSyncService.getWithSync(resultTask.id, (t) -> {
            t.setAssignedOn(System.currentTimeMillis());
            t.setProcessorId(processor.getId());
            t.setExecState(EnumsApi.TaskExecState.IN_PROGRESS.value);
            t.setResultResourceScheduledOn(0);
            taskTransactionalService.save(t);
            return null;
        });
*/
        resultTask.setAssignedOn(System.currentTimeMillis());
        resultTask.setProcessorId(processor.getId());
        resultTask.setExecState(EnumsApi.TaskExecState.IN_PROGRESS.value);
        resultTask.setResultResourceScheduledOn(0);
        taskTransactionalService.save(resultTask);

        try {
            updateTaskExecStates(execContextCache.findById(execContextId), resultTask.getId(), EnumsApi.TaskExecState.IN_PROGRESS.value, null);
        } catch (ObjectOptimisticLockingFailureException e) {
            e.printStackTrace();
        }
        dispatcherEventService.publishTaskEvent(EnumsApi.DispatcherEventType.TASK_ASSIGNED, processor.getId(), resultTask.getId(), execContextId);

        return resultTask;
    }

    @Nullable
    public TaskImpl prepareVariables(TaskImpl task) {

        TaskParamsYaml taskParams = TaskParamsYamlUtils.BASE_YAML_UTILS.to(task.getParams());

        final Long execContextId = task.execContextId;
        ExecContextImpl execContext = execContextCache.findById(execContextId);
        if (execContext==null) {
            log.warn("#705.280 can't assign a new task in execContext with Id #"+ execContextId +". This execContext doesn't exist");
            return null;
        }
        ExecContextParamsYaml execContextParamsYaml = ExecContextParamsYamlUtils.BASE_YAML_UTILS.to(execContext.params);
        ExecContextParamsYaml.Process p = execContextParamsYaml.findProcess(taskParams.task.processCode);
        if (p==null) {
            log.warn("#705.300 can't find process '"+taskParams.task.processCode+"' in execContext with Id #"+ execContextId);
            return null;
        }

        // we dont need to create inputs because all inputs are outputs of previous processes,
        // except globals and startInputAs
        // but we need to initialize descriptor of input variable
        p.inputs.stream()
                .map(v -> taskHelperService.toInputVariable(v, taskParams.task.taskContextId, execContextId))
                .collect(Collectors.toCollection(()->taskParams.task.inputs));

        return taskTransactionalService.persistOutputVariables(task, taskParams, execContext, p);
    }


    public static List<Long> getIdsForSearch(List<ExecContextData.TaskVertex> vertices, int page, int pageSize) {
        final int fromIndex = page * pageSize;
        if (vertices.size()<=fromIndex) {
            return List.of();
        }
        int toIndex = fromIndex + (vertices.size()-pageSize>=fromIndex ? pageSize : vertices.size() - fromIndex);
        return vertices.subList(fromIndex, toIndex).stream()
                .map(v -> v.taskId)
                .collect(Collectors.toList());
    }

    private static boolean gitUnavailable(TaskParamsYaml.TaskYaml task, boolean gitNotInstalled) {
        if (task.function.sourcing == EnumsApi.FunctionSourcing.git && gitNotInstalled) {
            return true;
        }
        for (TaskParamsYaml.FunctionConfig preFunction : task.preFunctions) {
            if (preFunction.sourcing == EnumsApi.FunctionSourcing.git && gitNotInstalled) {
                return true;
            }
        }
        for (TaskParamsYaml.FunctionConfig postFunction : task.postFunctions) {
            if (postFunction.sourcing == EnumsApi.FunctionSourcing.git && gitNotInstalled) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public EnumsApi.TaskProducingStatus toProducing(Long execContextId, ExecContextService execContextService) {
        return execContextSyncService.getWithSync(execContextId, () -> {
            ExecContextImpl execContext = execContextCache.findById(execContextId);
            if (execContext==null) {
                return EnumsApi.TaskProducingStatus.EXEC_CONTEXT_NOT_FOUND_ERROR;
            }
            if (execContext.state == EnumsApi.ExecContextState.PRODUCING.code) {
                return EnumsApi.TaskProducingStatus.OK;
            }
            execContext.setState(EnumsApi.ExecContextState.PRODUCING.code);
            execContextCache.save(execContext);
            return EnumsApi.TaskProducingStatus.OK;
        });
    }

    private void storeExecResultInternal(ProcessorCommParamsYaml.ReportTaskProcessingResult.SimpleTaskExecResult result) {
        FunctionApiData.FunctionExec functionExec = FunctionExecUtils.to(result.getResult());
        if (functionExec==null) {
            String es = "#303.045 Task #" + result.taskId + " has empty execResult";
            log.info(es);
            functionExec = new FunctionApiData.FunctionExec();
        }
        FunctionApiData.SystemExecResult systemExecResult = functionExec.generalExec!=null ? functionExec.generalExec : functionExec.exec;
        if (!systemExecResult.isOk) {
            log.warn("#303.050 Task #{} finished with error, functionCode: {}, console: {}",
                    result.taskId,
                    systemExecResult.functionCode,
                    StringUtils.isNotBlank(systemExecResult.console) ? systemExecResult.console : "<console output is empty>");
        }
        try {
            EnumsApi.TaskExecState state = functionExec.allFunctionsAreOk() ? EnumsApi.TaskExecState.OK : EnumsApi.TaskExecState.ERROR;
            Task t = prepareAndSaveTask(result, state);
            if (t==null) {
                return;
            }
            dispatcherEventService.publishTaskEvent(
                    state==EnumsApi.TaskExecState.OK ? EnumsApi.DispatcherEventType.TASK_FINISHED : EnumsApi.DispatcherEventType.TASK_ERROR,
                    null, result.taskId, t.getExecContextId());

            TaskParamsYaml tpy = TaskParamsYamlUtils.BASE_YAML_UTILS.to(t.getParams());
            if (state==EnumsApi.TaskExecState.ERROR) {
                finishWithError(
                        t.getId(),
                        StringUtils.isNotBlank(systemExecResult.console) ? systemExecResult.console : "<console output is empty>",
                        t.getExecContextId(), tpy.task.taskContextId);
            }
            else {
                updateTaskExecStates( execContextCache.findById(t.getExecContextId()), t.getId(), t.getExecState(), tpy.task.taskContextId);
            }

        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("#303.060 !!!NEED TO INVESTIGATE. Error while storing result of execution of task, taskId: {}, error: {}", result.taskId, e.toString());
            log.error("#303.061 ObjectOptimisticLockingFailureException", e);
            ExceptionUtils.rethrow(e);
        }
    }

    @Nullable
    private Task prepareAndSaveTask(ProcessorCommParamsYaml.ReportTaskProcessingResult.SimpleTaskExecResult result, EnumsApi.TaskExecState state) {
        TaskImpl task = taskRepository.findById(result.taskId).orElse(null);
//        return taskSyncService.getWithSync(result.taskId, (task) -> {
            if (task==null) {
                log.warn("#303.110 Can't find Task for Id: {}", result.taskId);
                return null;
            }
            task.setExecState(state.value);

            if (state==EnumsApi.TaskExecState.ERROR) {
                task.setCompleted(true);
                task.setCompletedOn(System.currentTimeMillis());
                task.setResultReceived(true);
            }
            else {
                task.setResultResourceScheduledOn(System.currentTimeMillis());
                TaskParamsYaml yaml = TaskParamsYamlUtils.BASE_YAML_UTILS.to(task.getParams());
                // if there isn't any output variable which has to be uploaded to dispatcher then complete this task
                if (yaml.task.outputs.stream().noneMatch(o->o.sourcing== EnumsApi.DataSourcing.dispatcher)) {
                    task.setCompleted(true);
                    task.setCompletedOn(System.currentTimeMillis());
                }
            }
            task.setFunctionExecResults(result.getResult());
            task = taskTransactionalService.save(task);

            return task;
//        });
    }

}
