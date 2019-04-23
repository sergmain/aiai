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

package aiai.ai.launchpad.flow;

import aiai.ai.launchpad.beans.Flow;
import aiai.ai.launchpad.data.FlowData;
import aiai.ai.launchpad.data.OperationStatusRest;
import aiai.ai.utils.ControllerUtils;
import aiai.api.v1.EnumsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("Duplicates")
@Controller
@RequestMapping("/launchpad/flow")
@Slf4j
@Profile("launchpad")
public class FlowController {

    private static final String REDIRECT_LAUNCHPAD_FLOW_FLOWS = "redirect:/launchpad/flow/flows";

    private final FlowTopLevelService flowTopLevelService;

    public FlowController(FlowTopLevelService flowTopLevelService) {
        this.flowTopLevelService = flowTopLevelService;
    }

    @GetMapping("/flows")
    public String flows(Model model, @PageableDefault(size = 5) Pageable pageable,
                        @ModelAttribute("infoMessages") final ArrayList<String> infoMessages,
                        @ModelAttribute("errorMessage") final ArrayList<String> errorMessage) {
        FlowData.FlowsResult flowsResultRest = flowTopLevelService.getFlows(pageable);
        ControllerUtils.addMessagesToModel(model, flowsResultRest);
        model.addAttribute("result", flowsResultRest);
        return "launchpad/flow/flows";
    }

    // for AJAX
    @PostMapping("/flows-part")
    public String flowsPart(Model model, @PageableDefault(size = 10) Pageable pageable) {
        FlowData.FlowsResult flowsResultRest = flowTopLevelService.getFlows(pageable);
        model.addAttribute("result", flowsResultRest);
        return "launchpad/flow/flows :: table";
    }

    @GetMapping(value = "/flow-add")
    public String add(@ModelAttribute("flow") Flow flow) {
        return "launchpad/flow/flow-add";
    }

    @SuppressWarnings("Duplicates")
    @GetMapping(value = "/flow-edit/{id}")
    public String edit(@PathVariable Long id, Model model, final RedirectAttributes redirectAttributes) {
        FlowData.FlowResult flowResultRest = flowTopLevelService.getFlow(id);
        if (flowResultRest.status== EnumsApi.FlowValidateStatus.FLOW_NOT_FOUND_ERROR) {
            redirectAttributes.addFlashAttribute("errorMessage", flowResultRest.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }
        model.addAttribute("flow", flowResultRest.flow);
        return "launchpad/flow/flow-edit";
    }

    @SuppressWarnings("Duplicates")
    @GetMapping(value = "/flow-validate/{id}")
    public String validate(@PathVariable Long id, Model model, final RedirectAttributes redirectAttributes) {
        FlowData.FlowResult flowResultRest = flowTopLevelService.validateFlow(id);
        if (flowResultRest.status== EnumsApi.FlowValidateStatus.FLOW_NOT_FOUND_ERROR) {
            redirectAttributes.addFlashAttribute("errorMessage", flowResultRest.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }

        model.addAttribute("flow", flowResultRest.flow);
        model.addAttribute("infoMessages", flowResultRest.infoMessages);
        model.addAttribute("errorMessage", flowResultRest.errorMessages);
        return "launchpad/flow/flow-edit";
    }

    @PostMapping("/flow-add-commit")
    public String addFormCommit(Model model, Flow flow, final RedirectAttributes redirectAttributes) {
        FlowData.FlowResult flowResultRest = flowTopLevelService.addFlow(flow);
        if (flowResultRest.isErrorMessages()) {
            model.addAttribute("errorMessage", flowResultRest.errorMessages);
            return "launchpad/flow/flow-add";
        }

        if (flowResultRest.status== EnumsApi.FlowValidateStatus.OK ) {
            redirectAttributes.addFlashAttribute("infoMessages", Collections.singletonList("Validation result: OK"));
        }
        return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
    }

    @PostMapping("/flow-edit-commit")
    public String editFormCommit(Model model, Flow flowModel, final RedirectAttributes redirectAttributes) {
        FlowData.FlowResult flowResultRest = flowTopLevelService.updateFlow(flowModel);
        if (flowResultRest.isErrorMessages()) {
            model.addAttribute("errorMessage", flowResultRest.errorMessages);
            return "redirect:/launchpad/flow/flow-edit/"+flowResultRest.flow.getId();
        }

        if (flowResultRest.status== EnumsApi.FlowValidateStatus.OK ) {
            redirectAttributes.addFlashAttribute("infoMessages", Collections.singletonList("Validation result: OK"));
        }
        return "redirect:/launchpad/flow/flow-edit/"+flowResultRest.flow.getId();
    }

    @SuppressWarnings("Duplicates")
    @GetMapping("/flow-delete/{id}")
    public String delete(@PathVariable Long id, Model model, final RedirectAttributes redirectAttributes) {
        FlowData.FlowResult flowResultRest = flowTopLevelService.getFlow(id);
        if (flowResultRest.status== EnumsApi.FlowValidateStatus.FLOW_NOT_FOUND_ERROR) {
            redirectAttributes.addFlashAttribute("errorMessage", flowResultRest.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }
        model.addAttribute("flow", flowResultRest.flow);
        return "launchpad/flow/flow-delete";
    }

    @PostMapping("/flow-delete-commit")
    public String deleteCommit(Long id, final RedirectAttributes redirectAttributes) {
        OperationStatusRest operationStatusRest = flowTopLevelService.deleteFlowById(id);
        if (operationStatusRest.isErrorMessages()) {
            redirectAttributes.addFlashAttribute("errorMessage", Collections.singletonList("#560.40 flow wasn't found, id: "+id) );
        }
        return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
    }

    // ============= Flow instances =============

    @GetMapping("/flow-instances/{id}")
    public String flowInstances(Model model, @PathVariable Long id, @PageableDefault(size = 5) Pageable pageable, @ModelAttribute("errorMessage") final String errorMessage) {
        model.addAttribute("result", flowTopLevelService.getFlowInstances(id, pageable));
        return "launchpad/flow/flow-instances";
    }

    // for AJAX
    @PostMapping("/flow-instances-part/{id}")
    public String flowInstancesPart(Model model, @PathVariable Long id, @PageableDefault(size = 10) Pageable pageable) {
        model.addAttribute("result", flowTopLevelService.getFlowInstances(id, pageable));
        return "launchpad/flow/flow-instances :: table";
    }

    @SuppressWarnings("Duplicates")
    @GetMapping(value = "/flow-instance-add/{id}")
    public String flowInstanceAdd(@ModelAttribute("result") FlowData.FlowResult result, @PathVariable Long id, final RedirectAttributes redirectAttributes) {
        FlowData.FlowResult flowResultRest = flowTopLevelService.getFlow(id);
        if (flowResultRest.status== EnumsApi.FlowValidateStatus.FLOW_NOT_FOUND_ERROR) {
            redirectAttributes.addFlashAttribute("errorMessage", flowResultRest.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }
        result.flow = flowResultRest.flow;
        return "launchpad/flow/flow-instance-add";
    }

    @PostMapping("/flow-instance-add-commit")
    public String flowInstanceAddCommit(@ModelAttribute("result") FlowData.FlowResult result, Long flowId, String poolCode, String inputResourceParams, final RedirectAttributes redirectAttributes) {
        FlowData.FlowInstanceResult flowInstanceResultRest = flowTopLevelService.addFlowInstance(flowId, poolCode, inputResourceParams);
        result.flow = flowInstanceResultRest.flow;
        if (result.flow == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "#560.60 flow wasn't found, flowId: " + flowId);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }

        if (flowInstanceResultRest.isErrorMessages()) {
            redirectAttributes.addFlashAttribute("errorMessage", flowInstanceResultRest.errorMessages);
        }
        return "redirect:/launchpad/flow/flow-instances/" + flowId;
    }

    @GetMapping("/flow-instance-delete/{flowId}/{flowInstanceId}")
    public String flowInstanceDelete(Model model, @PathVariable Long flowId, @PathVariable Long flowInstanceId, final RedirectAttributes redirectAttributes) {
        FlowData.FlowInstanceResult result = flowTopLevelService.getFlowInstanceExtended(flowInstanceId);
        if (result.isErrorMessages()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }
        model.addAttribute("result", result);
        return "launchpad/flow/flow-instance-delete";
    }

    @PostMapping("/flow-instance-delete-commit")
    public String flowInstanceDeleteCommit(Long flowId, Long flowInstanceId, final RedirectAttributes redirectAttributes) {
        OperationStatusRest operationStatusRest = flowTopLevelService.deleteFlowInstanceById(flowId, flowInstanceId);
        if (operationStatusRest.isErrorMessages()) {
            redirectAttributes.addFlashAttribute("errorMessage", operationStatusRest.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }
        return "redirect:/launchpad/flow/flow-instances/"+ flowId;
    }

    @GetMapping("/flow-instance-target-exec-state/{flowId}/{state}/{id}")
    public String flowInstanceTargetExecState(@PathVariable Long flowId, @PathVariable String state, @PathVariable Long id, final RedirectAttributes redirectAttributes) {
        OperationStatusRest operationStatusRest = flowTopLevelService.changeFlowInstanceExecState(state, id);
        if (operationStatusRest.isErrorMessages()) {
            redirectAttributes.addFlashAttribute("errorMessage", operationStatusRest.errorMessages);
            return REDIRECT_LAUNCHPAD_FLOW_FLOWS;
        }
        return "redirect:/launchpad/flow/flow-instances/" + flowId;
    }

}
