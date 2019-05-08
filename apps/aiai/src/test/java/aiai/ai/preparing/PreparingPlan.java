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

package aiai.ai.preparing;

import aiai.ai.Consts;
import aiai.ai.Enums;
import aiai.ai.plan.TaskCollector;
import aiai.ai.yaml.input_resource_param.InputResourceParamUtils;
import aiai.api.v1.launchpad.Process;
import aiai.ai.launchpad.beans.Plan;
import aiai.ai.launchpad.beans.Workbook;
import aiai.ai.launchpad.beans.Snippet;
import aiai.ai.launchpad.binary_data.BinaryDataService;
import aiai.ai.launchpad.plan.PlanCache;
import aiai.ai.launchpad.plan.PlanService;
import aiai.ai.launchpad.repositories.WorkbookRepository;
import aiai.ai.launchpad.repositories.PlanRepository;
import aiai.ai.launchpad.snippet.SnippetCache;
import aiai.ai.launchpad.task.TaskPersistencer;
import aiai.ai.yaml.plan.PlanYaml;
import aiai.ai.yaml.plan.PlanYamlUtils;
import aiai.ai.yaml.input_resource_param.InputResourceParam;
import aiai.api.v1.EnumsApi;
import aiai.apps.commons.yaml.snippet.SnippetConfig;
import aiai.apps.commons.yaml.snippet.SnippetConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

@Slf4j
public abstract class PreparingPlan extends PreparingExperiment {

    @Autowired
    public PlanCache planCache;

    @Autowired
    public PlanRepository planRepository;

    @Autowired
    public WorkbookRepository workbookRepository;

    @Autowired
    public PlanService planService;

    @Autowired
    public SnippetCache snippetCache;

    @Autowired
    public PlanYamlUtils planYamlUtils;

    @Autowired
    public TaskCollector taskCollector;

    @Autowired
    public TaskPersistencer taskPersistencer;

    public Plan plan = null;
    public PlanYaml planYaml = null;
    public Snippet s1 = null;
    public Snippet s2 = null;
    public Snippet s3 = null;
    public Snippet s4 = null;
    public Snippet s5 = null;
    public Workbook workbook = null;

    public InputResourceParam inputResourceParam;


    public abstract String getPlanParamsAsYaml();

    public String getPlanParamsAsYaml_Simple() {
        planYaml = new PlanYaml();
        {
            Process p = new Process();
            p.type = EnumsApi.ProcessType.FILE_PROCESSING;
            p.name = "assembly raw file";
            p.code = "assembly-raw-file";

            p.snippetCodes = Collections.singletonList("snippet-01:1.1");
            p.collectResources = false;
            p.outputType = "assembled-raw-output";

            planYaml.processes.add(p);
        }
        {
            Process p = new Process();
            p.type = EnumsApi.ProcessType.FILE_PROCESSING;
            p.name = "dataset processing";
            p.code = "dataset-processing";

            p.snippetCodes = Collections.singletonList("snippet-02:1.1");
            p.collectResources = true;
            p.outputType = "dataset-processing-output";

            planYaml.processes.add(p);
        }
        {
            Process p = new Process();
            p.type = EnumsApi.ProcessType.FILE_PROCESSING;
            p.name = "feature processing";
            p.code = "feature-processing";

            p.snippetCodes = Arrays.asList("snippet-03:1.1", "snippet-04:1.1", "snippet-05:1.1");
            p.parallelExec = true;
            p.collectResources = true;
            p.outputType = "feature-output";

            planYaml.processes.add(p);
        }
        {
            Process p = new Process();
            p.type = EnumsApi.ProcessType.EXPERIMENT;
            p.name = "experiment";
            p.code = PreparingExperiment.TEST_EXPERIMENT_CODE_01;

            p.metas.addAll(
                    Arrays.asList(
                            new Process.Meta("assembled-raw", "assembled-raw-output", null),
                            new Process.Meta("dataset", "dataset-processing-output", null),
                            new Process.Meta("feature", "feature-output", null)
                    )
            );

            planYaml.processes.add(p);
        }

        String yaml = planYamlUtils.toString(planYaml);
        System.out.println(yaml);
        return yaml;
    }

    @Autowired
    private BinaryDataService binaryDataService;

    public static final String INPUT_POOL_CODE = "test-input-pool-code";
    public static final String INPUT_RESOURCE_CODE = "test-input-resource-code-";

    @Before
    public void beforePreparingPlan() {
        assertTrue(globals.isUnitTesting);

        s1 = createSnippet("snippet-01:1.1");
        s2 = createSnippet("snippet-02:1.1");
        s3 = createSnippet("snippet-03:1.1");
        s4 = createSnippet("snippet-04:1.1");
        s5 = createSnippet("snippet-05:1.1");

        plan = new Plan();
        plan.setCode("test-plan-code");

        String params = getPlanParamsAsYaml();
        plan.setParams(params);
        plan.setCreatedOn(System.currentTimeMillis());


        Plan tempPlan = planRepository.findByCode(plan.getCode());
        if (tempPlan!=null) {
            planCache.deleteById(tempPlan.getId());
        }
        planCache.save(plan);

        byte[] bytes = "A resource for input pool".getBytes();

        binaryDataService.save(new ByteArrayInputStream(bytes), bytes.length,
                Enums.BinaryDataType.DATA,INPUT_RESOURCE_CODE+1, INPUT_POOL_CODE,
                true, "file-01.txt",
                null);
        binaryDataService.save(new ByteArrayInputStream(bytes), bytes.length,
                Enums.BinaryDataType.DATA,INPUT_RESOURCE_CODE+2, INPUT_POOL_CODE,
                true, "file-02.txt",
                null);
        binaryDataService.save(new ByteArrayInputStream(bytes), bytes.length,
                Enums.BinaryDataType.DATA,INPUT_RESOURCE_CODE+3, INPUT_POOL_CODE,
                true, "file-03.txt",
                null);

        inputResourceParam = new InputResourceParam();
        inputResourceParam.poolCodes = new HashMap<>();
        inputResourceParam.poolCodes.computeIfAbsent(Consts.WORKBOOK_INPUT_TYPE, o-> new ArrayList<>()).add(INPUT_POOL_CODE);
    }

    private Snippet createSnippet(String snippetCode) {
        SnippetConfig sc = new SnippetConfig();
        sc.code = snippetCode;
        sc.type = snippetCode + "-type";
        sc.file = null;
        sc.setEnv("env-"+snippetCode);
        sc.sourcing = EnumsApi.SnippetSourcing.station;;
        sc.metrics = false;

        sc.info.setSigned(false);
        sc.info.setLength(1000);

        Snippet s = new Snippet();
        Snippet sn = snippetRepository.findByCode(snippetCode);
        if (sn!=null) {
            snippetCache.delete(sn);
        }
        s.setCode(snippetCode);
        s.setType(sc.type);
        s.setParams(SnippetConfigUtils.toString(sc));

        snippetCache.save(s);
        return s;
    }

    @After
    public void afterPreparingPlan() {
        if (plan!=null) {
            try {
                planCache.deleteById(plan.getId());
            } catch (Throwable th) {
                log.error("Error while planCache.deleteById()", th);
            }
        }
        deleteSnippet(s1);
        deleteSnippet(s2);
        deleteSnippet(s3);
        deleteSnippet(s4);
        deleteSnippet(s5);
        if (workbook!=null) {
            try {
                workbookRepository.deleteById(workbook.getId());
            } catch (Throwable th) {
                log.error("Error while workbookRepository.deleteById()", th);
            }
            try {
                taskRepository.deleteByWorkbookId(workbook.getId());
            } catch (ObjectOptimisticLockingFailureException th) {
                //
            } catch (Throwable th) {
                log.error("Error while taskRepository.deleteByWorkbookId()", th);
            }
        }
        try {
            binaryDataService.deleteByPoolCodeAndDataType(INPUT_POOL_CODE, Enums.BinaryDataType.DATA);
        } catch (Throwable th) {
            log.error("error", th);
        }
    }

    public PlanService.TaskProducingResult produceTasksForTest() {
        assertFalse(planYaml.processes.isEmpty());
        assertEquals(EnumsApi.ProcessType.EXPERIMENT, planYaml.processes.get(planYaml.processes.size()-1).type);

        EnumsApi.PlanValidateStatus status = planService.validate(plan);
        assertEquals(EnumsApi.PlanValidateStatus.OK, status);

        PlanService.TaskProducingResult result = planService.createWorkbook(plan.getId(), InputResourceParamUtils.toString(inputResourceParam));
        workbook = result.workbook;

        assertEquals(EnumsApi.PlanProducingStatus.OK, result.planProducingStatus);
        assertNotNull(workbook);
        assertEquals(Enums.WorkbookExecState.NONE.code, workbook.execState);


        EnumsApi.PlanProducingStatus producingStatus = planService.toProducing(workbook);
        assertEquals(EnumsApi.PlanProducingStatus.OK, producingStatus);
        assertEquals(Enums.WorkbookExecState.PRODUCING.code, workbook.execState);

        result = planService.produceAllTasks(true, plan, workbook);
        workbook = result.workbook;
        assertEquals(EnumsApi.PlanProducingStatus.OK, result.planProducingStatus);
        assertEquals(Enums.WorkbookExecState.PRODUCED.code, workbook.execState);

        experiment = experimentCache.findById(experiment.getId());
        return result;
    }

    private void deleteSnippet(Snippet s) {
        if (s!=null) {
            try {
                snippetCache.delete(s);
            } catch (Throwable th) {
                log.error("Error", th);
            }
        }
    }
}