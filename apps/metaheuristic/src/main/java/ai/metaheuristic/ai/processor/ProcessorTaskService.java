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
package ai.metaheuristic.ai.processor;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.Globals;
import ai.metaheuristic.ai.processor.env.EnvService;
import ai.metaheuristic.ai.utils.DigitUtils;
import ai.metaheuristic.ai.yaml.communication.processor.ProcessorCommParamsYaml;
import ai.metaheuristic.ai.yaml.metadata.Metadata;
import ai.metaheuristic.ai.yaml.function_exec.FunctionExecUtils;
import ai.metaheuristic.ai.yaml.processor_task.ProcessorTask;
import ai.metaheuristic.ai.yaml.processor_task.ProcessorTaskUtils;
import ai.metaheuristic.api.ConstsApi;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.FunctionApiData;
import ai.metaheuristic.api.data.Meta;
import ai.metaheuristic.api.data.task.TaskParamsYaml;
import ai.metaheuristic.commons.CommonConsts;
import ai.metaheuristic.commons.S;
import ai.metaheuristic.commons.utils.MetaUtils;
import ai.metaheuristic.commons.yaml.YamlUtils;
import ai.metaheuristic.commons.yaml.ml.fitting.FittingYaml;
import ai.metaheuristic.commons.yaml.ml.fitting.FittingYamlUtils;
import ai.metaheuristic.commons.yaml.task.TaskParamsYamlUtils;
import ai.metaheuristic.commons.yaml.task_ml.metrics.Metrics;
import ai.metaheuristic.commons.yaml.task_ml.metrics.MetricsUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"UnnecessaryLocalVariable", "WeakerAccess"})
@Service
@Slf4j
@Profile("processor")
@RequiredArgsConstructor
public class ProcessorTaskService {

    private final Globals globals;
    private final CurrentExecState currentExecState;
    private final MetadataService metadataService;
    private final EnvService envService;

    private final Map<String, Map<Long, ProcessorTask>> map = new ConcurrentHashMap<>();

    @PostConstruct
    public void postConstruct() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.processorTaskDir.exists()) {
            return;
        }
        try {
            Files.list(globals.processorTaskDir.toPath()).forEach(top -> {
                try {
                    String dispatcherUrl = metadataService.findHostByCode(top.toFile().getName());
                    if (dispatcherUrl==null) {
                        return;
                    }
                    Files.list(top).forEach(p -> {
                        final File taskGroupDir = p.toFile();
                        if (!taskGroupDir.isDirectory()) {
                            return;
                        }
                        try {
                            AtomicBoolean isEmpty = new AtomicBoolean(true);
                            Files.list(p).forEach(s -> {
                                isEmpty.set(false);
                                String groupDirName = taskGroupDir.getName();
                                final File currDir = s.toFile();
                                String name = currDir.getName();
                                long taskId = Long.parseLong(groupDirName) * DigitUtils.DIV + Long.parseLong(name);
                                log.info("Found dir of task with id: {}, {}, {}", taskId, groupDirName, name);
                                File taskYamlFile = new File(currDir, Consts.TASK_YAML);
                                if (!taskYamlFile.exists() || taskYamlFile.length()==0L) {
                                    deleteDir(currDir, "Delete not valid dir of task " + s+", exist: "+taskYamlFile.exists()+", length: " +taskYamlFile.length());
                                    return;
                                }

                                try(FileInputStream fis = new FileInputStream(taskYamlFile)) {
                                    ProcessorTask task = ProcessorTaskUtils.to(fis);
                                    if (S.b(task.dispatcherUrl)) {
                                        deleteDir(currDir, "#713.005 Delete not valid dir of task " + s);
                                        log.warn("#713.007 task #{} from dispatcher {} was deleted from disk because dispatcherUrl field was empty", taskId, dispatcherUrl);
                                        return;
                                    }
                                    getMapForDispatcherUrl(dispatcherUrl).put(taskId, task);

                                    // fix state of task
                                    FunctionApiData.FunctionExec functionExec = FunctionExecUtils.to(task.getFunctionExecResult());
                                    if (functionExec !=null &&
                                            ((functionExec.generalExec!=null && !functionExec.exec.isOk ) ||
                                                    (functionExec.generalExec!=null && !functionExec.generalExec.isOk))) {
                                        markAsFinished(dispatcherUrl, taskId, functionExec);
                                    }
                                }
                                catch (IOException e) {
                                    String es = "#713.010 Error";
                                    log.error(es, e);
                                    throw new RuntimeException(es, e);
                                }
                                catch (YAMLException e) {
                                    String es = "#713.020 yaml Error: " + e.getMessage();
                                    log.warn(es, e);
                                    deleteDir(currDir, "Delete not valid dir of task " + s);
                                }
                            });
                        }
                        catch (IOException e) {
                            String es = "#713.030 Error";
                            log.error(es, e);
                            throw new RuntimeException(es, e);
                        }
                    });
                } catch (IOException e) {
                    String es = "#713.040 Error";
                    log.error(es, e);
                    throw new RuntimeException(es, e);
                }
            });
        }
        catch (IOException e) {
            String es = "#713.050 Error";
            log.error(es, e);
            throw new RuntimeException(es, e);
        }
        //noinspection unused
        int i=0;
    }

    public static void deleteDir(@NonNull File f, @NonNull String info) {
        log.warn(info+", file: " + f.getAbsolutePath());
        try {
            if (f.exists()) {
                FileUtils.deleteDirectory(f);
            }
        } catch (IOException e) {
            log.warn("#713.060 Error while deleting dir {}, error: {}", f.getPath(), e.toString());
        }
    }

    public void setReportedOn(String dispatcherUrl, long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("setReportedOn({}, {})", dispatcherUrl, taskId);
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                log.error("#713.070 ProcessorRestTask wasn't found for Id " + taskId);
                return;
            }
            task.setReported(true);
            task.setReportedOn(System.currentTimeMillis());
            save(task);
        }
    }

    public void setDelivered(String dispatcherUrl, Long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("setDelivered({}, {})", dispatcherUrl, taskId);
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                log.error("#713.080 ProcessorTask wasn't found for Id {}", taskId);
                return;
            }
            if (task.delivered) {
                return;
            }

            task.setDelivered(true);
            // if function has finished with an error,
            // then we don't have to set isCompleted any more
            // because we've already marked this task as completed
            if (!task.isCompleted()) {
                task.setCompleted(task.isResourceUploaded());
            }
            save(task);
        }
    }

    public void setResourceUploadedAndCompleted(String dispatcherUrl, Long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("setResourceUploadedAndCompleted({}, {})", dispatcherUrl, taskId);
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                log.error("#713.090 ProcessorTask wasn't found for Id {}", taskId);
                return;
            }
            task.setResourceUploaded(true);
            task.setCompleted( task.isDelivered() );
            save(task);
        }
    }

    @SuppressWarnings("unused")
    public void setCompleted(String dispatcherUrl, Long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("setCompleted({}, {})", dispatcherUrl, taskId);
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                log.error("#713.100 ProcessorTask wasn't found for Id {}", taskId);
                return;
            }
            task.setCompleted(true);
            save(task);
        }
    }

    public List<ProcessorTask> getForReporting(String dispatcherUrl) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            Stream<ProcessorTask> stream = findAllByFinishedOnIsNotNull(dispatcherUrl);
            List<ProcessorTask> result = stream
                    .filter(processorTask -> !processorTask.isReported() ||
                            (!processorTask.isDelivered() &&
                                    (processorTask.getReportedOn() == null || (System.currentTimeMillis() - processorTask.getReportedOn()) > 60_000)))
                    .collect(Collectors.toList());
            return result;
        }
    }

    public ProcessorCommParamsYaml.ReportTaskProcessingResult reportTaskProcessingResult(String dispatcherUrl) {
        final List<ProcessorTask> list = getForReporting(dispatcherUrl);
        if (list.isEmpty()) {
            return null;
        }
        log.info("Number of tasks for reporting: " + list.size());
        final ProcessorCommParamsYaml.ReportTaskProcessingResult processingResult = new ProcessorCommParamsYaml.ReportTaskProcessingResult();
        for (ProcessorTask task : list) {
            if (task.isDelivered() && !task.isReported() ) {
                log.warn("#775.140 This state need to be investigated: (task.isDelivered() && !task.isReported())==true");
            }
            // TODO 2019-07-12 do we need to check against task.isReported()? isn't task.isDelivered() just enough?
            if (task.isDelivered() && task.isReported() ) {
                continue;
            }
            ProcessorCommParamsYaml.ReportTaskProcessingResult.MachineLearningTaskResult ml = null;
            Meta predictedData = MetaUtils.getMeta(task.metas, Consts.META_PREDICTED_DATA);
            if (task.getMetrics()!=null || predictedData!=null) {
                ml = new ProcessorCommParamsYaml.ReportTaskProcessingResult.MachineLearningTaskResult(
                        task.getMetrics(), predictedData.getValue(), EnumsApi.Fitting.of(MetaUtils.getValue(task.metas, Consts.META_FITTED)));
            }
            final ProcessorCommParamsYaml.ReportTaskProcessingResult.SimpleTaskExecResult result =
                    new ProcessorCommParamsYaml.ReportTaskProcessingResult.SimpleTaskExecResult(task.getTaskId(), task.getFunctionExecResult(), ml);
            processingResult.results.add(result);
            setReportedOn(dispatcherUrl, task.taskId);
        }
        return processingResult;
    }

    public void markAsFinishedWithError(String dispatcherUrl, long taskId, String es) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            markAsFinished(dispatcherUrl, taskId,
                    new FunctionApiData.FunctionExec(
                            null, null, null,
                            new FunctionApiData.SystemExecResult("system-error", false, -991, es)));
        }
    }

    void markAsFinished(String dispatcherUrl, Long taskId, FunctionApiData.FunctionExec functionExec) {

        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("markAsFinished({}, {})", dispatcherUrl, taskId);
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                log.error("#713.110 ProcessorTask wasn't found for Id #" + taskId);
            } else {
                if (task.getLaunchedOn()==null) {
                    log.info("#713.113 task #{} doesn't have the launchedOn as inited", taskId);
                    task.setLaunchedOn(System.currentTimeMillis());
                }
                if (!functionExec.allFunctionsAreOk()) {
                    log.info("#713.115 task #{} was finished with an error, set completed to true", taskId);
                    // there are some problems with this task. mark it as completed
                    task.setCompleted(true);
                }
                task.setFinishedOn(System.currentTimeMillis());
                task.setDelivered(false);
                task.setReported(false);
                task.setFunctionExecResult(FunctionExecUtils.toString(functionExec));

                save(task);
            }
        }
    }

    void markAsAssetPrepared(String dispatcherUrl, Long taskId, boolean status) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("markAsAssetPrepared(dispatcherUrl: {}, taskId: {}, status: {})", dispatcherUrl, taskId, status);
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                log.error("#713.130 ProcessorTask wasn't found for Id {}", taskId);
            } else {
                task.setAssetsPrepared(status);
                save(task);
            }
        }
    }

    boolean isNeedNewTask(String dispatcherUrl, String processorId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            if (processorId == null) {
                return false;
            }
            // TODO 2019-10-24 need to optimize
            List<ProcessorTask> tasks = findAllByCompletedIsFalse(dispatcherUrl);
            for (ProcessorTask task : tasks) {
                // we don't need new task because execContext for this task is active
                // i.e. there is a non-completed task with active execContext
                // if execContext wasn't active we would need a new task
                if (currentExecState.isStarted(task.dispatcherUrl, task.execContextId)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void storePredictedData(String dispatcherUrl, ProcessorTask task, TaskParamsYaml.FunctionConfig functionConfig, File artifactDir) throws IOException {
        Meta m = MetaUtils.getMeta(functionConfig.metas, ConstsApi.META_MH_FITTING_DETECTION_SUPPORTED);
        if (MetaUtils.isTrue(m)) {
            log.info("storePredictedData(dispatcherUrl: {}, taskId: {}, function code: {})", dispatcherUrl, task.taskId, functionConfig.getCode());
            String data = getPredictedData(artifactDir);
            if (data!=null) {
                task.getMetas().add( new Meta(Consts.META_PREDICTED_DATA, data, null) );
                save(task);
            }
        }
    }

    public void storeFittingCheck(String dispatcherUrl, ProcessorTask task, TaskParamsYaml.FunctionConfig functionConfig, File artifactDir) throws IOException {
        if (functionConfig.type.equals(CommonConsts.CHECK_FITTING_TYPE)) {
           log.info("storeFittingCheck(dispatcherUrl: {}, taskId: {}, function code: {})", dispatcherUrl, task.taskId, functionConfig.getCode());
            FittingYaml fittingYaml = getFittingCheck(artifactDir);
            if (fittingYaml != null) {
                task.getMetas().add(new Meta(Consts.META_FITTED, fittingYaml.fitting.toString(), null));
                save(task);
            }
            else {
                log.error("#713.137 file with testing of fitting wasn't found, task #{}, artifact dir: {}", task.taskId, artifactDir.getAbsolutePath());
            }
        }
    }

    public void storeMetrics(String dispatcherUrl, ProcessorTask task, TaskParamsYaml.FunctionConfig functionConfig, File artifactDir) {
        // store metrics after predict only
        if (functionConfig.ml!=null && functionConfig.ml.metrics) {
            log.info("storeMetrics(dispatcherUrl: {}, taskId: {}, function code: {})", dispatcherUrl, task.taskId, functionConfig.getCode());
            Metrics metrics = new Metrics();
            File metricsFile = getMetricsFile(artifactDir);
            if (metricsFile!=null) {
                try {
                    String execMetrics = FileUtils.readFileToString(metricsFile, StandardCharsets.UTF_8);
                    metrics.setStatus(EnumsApi.MetricsStatus.Ok);
                    metrics.setMetrics(execMetrics);
                }
                catch (IOException e) {
                    log.error("#713.140 Error reading metrics file {}", metricsFile.getAbsolutePath());
                    metrics.setStatus(EnumsApi.MetricsStatus.Error);
                    metrics.setError(e.toString());
                }
            } else {
                metrics.setStatus(EnumsApi.MetricsStatus.NotFound);
            }
            task.setMetrics(MetricsUtils.toString(metrics));
            save(task);
        }
    }

    @SuppressWarnings("deprecation")
    private File getMetricsFile(File artifactDir) {
        File metricsFile = new File(artifactDir, Consts.MH_METRICS_FILE_NAME);
        if (metricsFile.exists()) {
            return metricsFile;
        }
        // let's try a file with legacy name
        metricsFile = new File(artifactDir, Consts.METRICS_FILE_NAME);
        return metricsFile.exists() ? metricsFile : null;
    }


    private String getPredictedData(File artifactDir) throws IOException {
        File file = new File(artifactDir, Consts.MH_PREDICTION_DATA_FILE_NAME);
        if (file.exists() && file.isFile()) {
            String data = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return data;
        }
        return null;
    }

    private FittingYaml getFittingCheck(File artifactDir) throws IOException {
        File file = new File(artifactDir, Consts.MH_FITTING_FILE_NAME);
        if (file.exists() && file.isFile()) {
            String yaml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return FittingYamlUtils.BASE_YAML_UTILS.to(yaml);
        }
        return null;
    }

    public List<ProcessorTask> findAllByCompletedIsFalse(String dispatcherUrl) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            List<ProcessorTask> list = new ArrayList<>();
            for (ProcessorTask task : getMapForDispatcherUrl(dispatcherUrl).values()) {
                if (!task.completed) {
                    list.add(task);
                }
            }
            return list;
        }
    }

    private Map<Long, ProcessorTask> getMapForDispatcherUrl(String dispatcherUrl) {
        return map.computeIfAbsent(dispatcherUrl, m -> new HashMap<>());
    }

    public List<ProcessorTask> findAllByCompetedIsFalseAndFinishedOnIsNullAndAssetsPreparedIs(boolean assetsPreparedStatus) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            List<ProcessorTask> list = new ArrayList<>();
            for (String dispatcherUrl : map.keySet()) {
                Map<Long, ProcessorTask> mapForDispatcherUrl = getMapForDispatcherUrl(dispatcherUrl);
                List<Long> forDelition = new ArrayList<>();
                for (ProcessorTask task : mapForDispatcherUrl.values()) {
                    if (S.b(task.dispatcherUrl)) {
                        forDelition.add(task.taskId);
                    }
                    if (!task.completed && task.finishedOn == null && task.assetsPrepared==assetsPreparedStatus) {
                        list.add(task);
                    }
                }
                forDelition.forEach(id-> {
                    log.warn("#713.147 task #{} from dispatcher {} was deleted from global map with tasks", id, dispatcherUrl);
                    mapForDispatcherUrl.remove(id);
                });
            }
            return list;
        }
    }

    private Stream<ProcessorTask> findAllByFinishedOnIsNotNull(String dispatcherUrl) {
        return getMapForDispatcherUrl(dispatcherUrl).values().stream().filter(o -> o.finishedOn!=null);
    }

    public ProcessorCommParamsYaml.ReportProcessorTaskStatus produceProcessorTaskStatus(String dispatcherUrl) {
        List<ProcessorCommParamsYaml.ReportProcessorTaskStatus.SimpleStatus> statuses = new ArrayList<>();
        List<ProcessorTask> list = findAll(dispatcherUrl);
        for (ProcessorTask task : list) {
            statuses.add( new ProcessorCommParamsYaml.ReportProcessorTaskStatus.SimpleStatus(task.getTaskId()));
        }
        return new ProcessorCommParamsYaml.ReportProcessorTaskStatus(statuses);
    }

    public void createTask(String dispatcherUrl, long taskId, Long execContextId, String params) {
        if (dispatcherUrl==null) {
            throw new IllegalStateException("#713.150 dispatcherUrl is null");
        }
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            log.info("Assign new task #{}, params:\n{}", taskId, params );
            Map<Long, ProcessorTask> mapForDispatcherUrl = getMapForDispatcherUrl(dispatcherUrl);
            ProcessorTask task = mapForDispatcherUrl.computeIfAbsent(taskId, k -> new ProcessorTask());

            task.taskId = taskId;
            task.execContextId = execContextId;
            task.params = params;
            task.metrics = null;
            task.functionExecResult = null;
            final TaskParamsYaml taskParamYaml = TaskParamsYamlUtils.BASE_YAML_UTILS.to(params);
            task.clean = taskParamYaml.taskYaml.clean;
            task.dispatcherUrl = dispatcherUrl;
            task.createdOn = System.currentTimeMillis();
            task.assetsPrepared = false;
            task.launchedOn = null;
            task.finishedOn = null;
            task.reportedOn = null;
            task.reported = false;
            task.delivered = false;
            task.resourceUploaded = false;
            task.completed = false;

            File dispatcherDir = new File(globals.processorTaskDir, metadataService.dispatcherUrlAsCode(dispatcherUrl).code);
            String path = getTaskPath(taskId);
            File taskDir = new File(dispatcherDir, path);
            try {
                //noinspection StatementWithEmptyBody
                if (taskDir.exists()) {
//                deleteOrRenameTaskDir(taskDir, taskYamlFile);
//                    FileUtils.deleteDirectory(taskDir);
                }
                else {
                    taskDir.mkdirs();
                }
                //noinspection ResultOfMethodCallIgnored
                taskDir.mkdirs();
                File taskYamlFile = new File(taskDir, Consts.TASK_YAML);
//                deleteYamlTaskFile(taskYamlFile);
                FileUtils.write(taskYamlFile, ProcessorTaskUtils.toString(task), Charsets.UTF_8, false);
            } catch (Throwable th) {
                String es = "#713.160 Error";
                log.error(es, th);
                throw new RuntimeException(es, th);
            }
        }
    }

    public ProcessorTask resetTask(String dispatcherUrl, Long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                return null;
            }
            task.setLaunchedOn(null);
            return save(task);
        }
    }

    public ProcessorTask setLaunchOn(String dispatcherUrl, long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            ProcessorTask task = findById(dispatcherUrl, taskId);
            if (task == null) {
                return null;
            }
            task.setLaunchedOn(System.currentTimeMillis());
            return save(task);
        }
    }

    private ProcessorTask save(ProcessorTask task) {
        File taskDir = prepareTaskDir(task.dispatcherUrl, task.taskId);
        File taskYaml = new File(taskDir, Consts.TASK_YAML);

        if (taskYaml.exists()) {
            log.trace("{} file exists. Make backup", taskYaml.getPath());
            File yamlFileBak = new File(taskDir, Consts.TASK_YAML + ".bak");
            //noinspection ResultOfMethodCallIgnored
            yamlFileBak.delete();
            if (taskYaml.exists()) {
                //noinspection ResultOfMethodCallIgnored
                taskYaml.renameTo(yamlFileBak);
            }
        }

        try {
            FileUtils.write(taskYaml, ProcessorTaskUtils.toString(task), Charsets.UTF_8, false);
        } catch (IOException e) {
            String es = "#713.200 Error while writing to file: " + taskYaml.getPath();
            log.error(es, e);
            throw new IllegalStateException(es, e);
        }
        return task;
    }

    public ProcessorTask findById(String dispatcherUrl, Long taskId) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            return getMapForDispatcherUrl(dispatcherUrl)
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().taskId == taskId)
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }
    }

    public List<ProcessorTask> findAll(String dispatcherUrl) {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            Collection<ProcessorTask> values = getMapForDispatcherUrl(dispatcherUrl).values();
            return List.copyOf(values);
        }
    }

    public List<ProcessorTask> findAll() {
        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            List<ProcessorTask> list = new ArrayList<>();
            for (String dispatcherUrl : map.keySet()) {
                list.addAll( getMapForDispatcherUrl(dispatcherUrl).values());
            }
            return list;
        }
    }

    public void delete(String dispatcherUrl, final long taskId) {
        Metadata.DispatcherInfo dispatcherCode = metadataService.dispatcherUrlAsCode(dispatcherUrl);

        synchronized (ProcessorSyncHolder.processorGlobalSync) {
            final String path = getTaskPath(taskId);

            final File dispatcherDir = new File(globals.processorTaskDir, dispatcherCode.code);
            final File taskDir = new File(dispatcherDir, path);
            try {
                if (taskDir.exists()) {
                    deleteDir(taskDir, "delete dir in ProcessorTaskService.delete()");
                }
                Map<Long, ProcessorTask> mapTask = getMapForDispatcherUrl(dispatcherUrl);
                log.debug("Does task present in map before deleting: {}", mapTask.containsKey(taskId));
                mapTask.remove(taskId);
                log.debug("Does task present in map after deleting: {}", mapTask.containsKey(taskId));
            } catch (Throwable th) {
                log.error("#713.210 Error deleting task " + taskId, th);
            }
        }
    }

    private String getTaskPath(long taskId) {
        DigitUtils.Power power = DigitUtils.getPower(taskId);
        return ""+power.power7+File.separatorChar+power.power4+File.separatorChar;
    }

    File prepareTaskDir(String dispatcherUrl, Long taskId) {
        Metadata.DispatcherInfo dispatcherCode = metadataService.dispatcherUrlAsCode(dispatcherUrl);
        return prepareTaskDir(dispatcherCode, taskId);
    }

    File prepareTaskDir(Metadata.DispatcherInfo dispatcherCode, Long taskId) {
        final File dispatcherDir = new File(globals.processorTaskDir, dispatcherCode.code);
        File taskDir = new File(dispatcherDir, getTaskPath(taskId));
        if (taskDir.exists()) {
            return taskDir;
        }
        //noinspection unused
        boolean status = taskDir.mkdirs();
        return taskDir;
    }

    File prepareTaskSubDir(File taskDir, String subDir) {
        File taskSubDir = new File(taskDir, subDir);
        //noinspection ResultOfMethodCallIgnored
        taskSubDir.mkdirs();
        if (!taskSubDir.exists()) {
            log.warn("#713.220 Can't create taskSubDir: {}", taskSubDir.getAbsolutePath());
            return null;
        }
        return taskSubDir;
    }

    @Data
    @AllArgsConstructor
    public static class EnvYamlShort {
        public final Map<String, String> envs;
    }

    private static Yaml getYamlForEnvYamlShort() {
        return YamlUtils.init(EnvYamlShort.class);
    }

    private static String envYamlShortToString(EnvYamlShort envYamlShort) {
        return YamlUtils.toString(envYamlShort, getYamlForEnvYamlShort());
    }

    public String prepareEnvironment(File artifactDir) {
        File envFile = new File(artifactDir, ConstsApi.MH_ENV_FILE);
        if (envFile.isDirectory()) {
            return "#713.220 path "+ artifactDir.getAbsolutePath()+" is dir, can't continue processing";
        }
        EnvYamlShort envYaml = new EnvYamlShort(envService.getEnvYaml().envs);
        final String newEnv = envYamlShortToString(envYaml);

        try {
            FileUtils.writeStringToFile(envFile, newEnv, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "#713.223 error creating "+ConstsApi.MH_ENV_FILE+", error: " + e.getMessage();
        }

        return null;
    }
}