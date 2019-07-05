/*
 * Metaheuristic, Copyright (C) 2017-2019  Serge Maslyukov
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

package ai.metaheuristic.ai.launchpad.experiment;

import ai.metaheuristic.ai.launchpad.beans.Experiment;
import ai.metaheuristic.ai.launchpad.plan.ProcessValidator;
import ai.metaheuristic.ai.launchpad.repositories.ExperimentRepository;
import ai.metaheuristic.ai.launchpad.repositories.WorkbookRepository;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.Meta;
import ai.metaheuristic.api.data.experiment.ExperimentParamsYaml;
import ai.metaheuristic.api.launchpad.Plan;
import ai.metaheuristic.api.launchpad.Workbook;
import ai.metaheuristic.api.launchpad.process.Process;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("launchpad")
@Slf4j
@RequiredArgsConstructor
public class ExperimentProcessValidator implements ProcessValidator {

    private final ExperimentRepository experimentRepository;
    private final ExperimentCache experimentCache;
    private final WorkbookRepository workbookRepository;

    // TODO experiment has to be stateless and have its own instances
    // TODO 2019.05.02 do we need an experiment to have its own instance still?
    // TODO 2019.07.04 current thought is that we don't need stateless experiment
    // TODO because each experiment has its own set of hyper parameters

    @Override
    public EnumsApi.PlanValidateStatus validate(Plan plan, Process process, boolean isFirst) {
        if (process.snippets!=null && process.snippets.size() > 0) {
            return EnumsApi.PlanValidateStatus.SNIPPET_ALREADY_PROVIDED_BY_EXPERIMENT_ERROR;
        }
        if (StringUtils.isBlank(process.code)) {
            return EnumsApi.PlanValidateStatus.SNIPPET_NOT_DEFINED_ERROR;
        }
        Long experimentId = experimentRepository.findIdByCode(process.code);
        if (experimentId==null) {
            return EnumsApi.PlanValidateStatus.EXPERIMENT_NOT_FOUND_ERROR;
        }
        Experiment e = experimentCache.findById(experimentId);
        if (e==null) {
            return EnumsApi.PlanValidateStatus.EXPERIMENT_NOT_FOUND_ERROR;
        }
        if (e.getWorkbookId()!=null) {
            Workbook workbook = workbookRepository.findById(e.getWorkbookId()).orElse(null);
            if (workbook != null) {
                if (!plan.getId().equals(workbook.getPlanId())) {
                    return EnumsApi.PlanValidateStatus.EXPERIMENT_ALREADY_STARTED_ERROR;
                }
            }
            else {
                return EnumsApi.PlanValidateStatus.WORKBOOK_DOESNT_EXIST_ERROR;
            }
        }
        ExperimentParamsYaml epy = e.getExperimentParamsYaml();

        if (StringUtils.isBlank(epy.experimentYaml.fitSnippet) || StringUtils.isBlank(epy.experimentYaml.predictSnippet)) {
            return EnumsApi.PlanValidateStatus.EXPERIMENT_HASNT_ALL_SNIPPETS_ERROR;
        }

        if (!isFirst) {
            if (process.metas == null || process.metas.isEmpty()) {
                return EnumsApi.PlanValidateStatus.EXPERIMENT_META_NOT_FOUND_ERROR;
            }

            Meta m1 = process.getMeta("dataset");
            if (m1 == null || StringUtils.isBlank(m1.getValue())) {
                return EnumsApi.PlanValidateStatus.EXPERIMENT_META_DATASET_NOT_FOUND_ERROR;
            }

            // TODO 2019.05.02 do we need this check?
//            Process.Meta m2 = process.getMeta("assembled-raw");
//            if (m2 == null || StringUtils.isBlank(m2.getValue())) {
//                return EnumsApi.PlanValidateStatus.EXPERIMENT_META_ASSEMBLED_RAW_NOT_FOUND_ERROR;
//            }

            Meta m3 = process.getMeta("feature");
            if (m3 == null || StringUtils.isBlank(m3.getValue())) {
                return EnumsApi.PlanValidateStatus.EXPERIMENT_META_FEATURE_NOT_FOUND_ERROR;
            }
        }
        return null;
    }
}
