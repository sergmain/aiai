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

package aiai.ai.launchpad.rest.v1;

import aiai.ai.launchpad.beans.Plan;
import aiai.ai.launchpad.data.PlanData;
import aiai.ai.launchpad.data.OperationStatusRest;
import aiai.ai.launchpad.plan.PlanTopLevelService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/v1/launchpad/plan")
@Profile("launchpad")
@CrossOrigin
//@CrossOrigin(origins="*", maxAge=3600)
public class PlanRestController {

    private final PlanTopLevelService planTopLevelService;

    public PlanRestController(PlanTopLevelService planTopLevelService) {
        this.planTopLevelService = planTopLevelService;
    }

    // ============= Plan =============

    @GetMapping("/plans")
    public PlanData.PlansResult plans(@PageableDefault(size = 5) Pageable pageable) {
        return planTopLevelService.getPlans(pageable);
    }

    @GetMapping(value = "/plan/{id}")
    public PlanData.PlanResult edit(@PathVariable Long id) {
        return planTopLevelService.getPlan(id);
    }

    @GetMapping(value = "/plan-validate/{id}")
    public PlanData.PlanResult validate(@PathVariable Long id) {
        return planTopLevelService.validatePlan(id);
    }

    @PostMapping("/plan-add-commit")
    public PlanData.PlanResult addFormCommit(@RequestBody Plan plan) {
        return planTopLevelService.addPlan(plan);
    }

    @PostMapping("/plan-edit-commit")
    public PlanData.PlanResult editFormCommit(@RequestBody Plan plan) {
        return planTopLevelService.updatePlan(plan);
    }

    @PostMapping("/plan-delete-commit")
    public OperationStatusRest deleteCommit(Long id) {
        return planTopLevelService.deletePlanById(id);
    }

    // ============= Workbooks =============

    @GetMapping("/workbooks/{id}")
    public PlanData.WorkbooksResult workbooks(@PathVariable Long id, @PageableDefault(size = 5) Pageable pageable) {
        return planTopLevelService.getWorkbooksOrderByCreatedOnDesc(id, pageable);
    }

    @PostMapping("/workbook-add-commit")
    public PlanData.WorkbookResult workbookAddCommit(Long planId, String poolCode, String inputResourceParams) {
        //noinspection UnnecessaryLocalVariable
        PlanData.WorkbookResult workbookResult = planTopLevelService.addWorkbook(planId, poolCode, inputResourceParams);
        return workbookResult;
    }

    @PostMapping("/workbook-create")
    public PlanData.TaskProducingResult createWorkbook(Long planId, String inputResourceParam) {
        return planTopLevelService.createWorkbook(planId, inputResourceParam);
    }

    @GetMapping(value = "/workbook/{planId}/{workbookId}")
    public PlanData.WorkbookResult workbookEdit(@SuppressWarnings("unused") @PathVariable Long planId, @PathVariable Long workbookId) {
        return planTopLevelService.getWorkbookExtended(workbookId);
    }

    @PostMapping("/workbook-delete-commit")
    public OperationStatusRest workbookDeleteCommit(Long planId, Long workbookId) {
        return planTopLevelService.deleteWorkbookById(planId, workbookId);
    }

    @GetMapping("/workbook-target-exec-state/{planId}/{state}/{id}")
    public OperationStatusRest workbookTargetExecState(@SuppressWarnings("unused") @PathVariable Long planId, @PathVariable String state, @PathVariable Long id) {
        return planTopLevelService.changeWorkbookExecState(state, id);
    }

    // ============= Service methods =============

    @GetMapping(value = "/emulate-producing-tasks/{workbookId}")
    public PlanData.TaskProducingResult emulateProducingTasks(@PathVariable Long workbookId) {
        return planTopLevelService.produceTasksWithoutPersistence(workbookId);
    }

    @GetMapping(value = "/create-all-tasks")
    public void createAllTasks() {
        planTopLevelService.createAllTasks();
    }

    @GetMapping(value = "/change-valid-status/{workbookId}/{status}")
    public OperationStatusRest changeValidStatus(@PathVariable Long workbookId, @PathVariable boolean status) {
        return planTopLevelService.changeValidStatus(workbookId, status);
    }



}