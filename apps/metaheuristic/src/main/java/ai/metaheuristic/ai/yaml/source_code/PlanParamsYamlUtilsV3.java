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

package ai.metaheuristic.ai.yaml.source_code;

import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.source_code.SourceCodeApiData;
import ai.metaheuristic.api.data.source_code.SourceCodeParamsYamlV3;
import ai.metaheuristic.api.data.source_code.SourceCodeParamsYamlV4;
import ai.metaheuristic.api.data_storage.DataStorageParams;
import ai.metaheuristic.api.launchpad.process.ProcessV3;
import ai.metaheuristic.api.launchpad.process.ProcessV4;
import ai.metaheuristic.api.launchpad.process.SnippetDefForPlanV4;
import ai.metaheuristic.commons.yaml.YamlUtils;
import ai.metaheuristic.commons.yaml.versioning.AbstractParamsYamlUtils;
import org.springframework.beans.BeanUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author Serge
 * Date: 6/17/2019
 * Time: 12:10 AM
 */
public class PlanParamsYamlUtilsV3
        extends AbstractParamsYamlUtils<SourceCodeParamsYamlV3, SourceCodeParamsYamlV4, PlanParamsYamlUtilsV4, Void, Void, Void> {

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public Yaml getYaml() {
        return YamlUtils.init(SourceCodeParamsYamlV3.class);
    }

    @Override
    public SourceCodeParamsYamlV4 upgradeTo(SourceCodeParamsYamlV3 pV3, Long ... vars) {
        SourceCodeParamsYamlV4 p = new SourceCodeParamsYamlV4();
        p.internalParams = pV3.internalParams;
        p.planYaml = new SourceCodeParamsYamlV4.SourceCodeYamlV4();
        if (pV3.planYaml.metas!=null){
            p.planYaml.metas = new ArrayList<>(pV3.planYaml.metas);
        }
        p.planYaml.clean = pV3.planYaml.clean;
        p.planYaml.processes = pV3.planYaml.processes.stream().map( o-> {
            ProcessV4 pr = new ProcessV4();
            BeanUtils.copyProperties(o, pr, "snippetCodes", "preSnippetCode", "postSnippetCode");
            if (o.snippetCodes!=null) {
                pr.snippets = new ArrayList<>();
                for (String snippetCode : o.snippetCodes) {
                    pr.snippets.add(new SnippetDefForPlanV4(snippetCode) );
                }
            }
            if (o.preSnippetCode!=null) {
                pr.preSnippets = new ArrayList<>();
                for (String snippetCode : o.preSnippetCode) {
                    pr.preSnippets.add(new SnippetDefForPlanV4(snippetCode) );
                }
            }
            if (o.postSnippetCode!=null) {
                pr.postSnippets = new ArrayList<>();
                for (String snippetCode : o.postSnippetCode) {
                    pr.postSnippets.add(new SnippetDefForPlanV4(snippetCode) );
                }
            }
            return pr;
        }).collect(Collectors.toList());
        return p;
    }

    @Override
    public Void downgradeTo(Void yaml) {
        // not supported
        return null;
    }

    @Override
    public PlanParamsYamlUtilsV4 nextUtil() {
        return (PlanParamsYamlUtilsV4) SourceCodeParamsYamlUtils.BASE_YAML_UTILS.getForVersion(4);
    }

    @Override
    public Void prevUtil() {
        // not supported
        return null;
    }

    @Override
    public String toString(SourceCodeParamsYamlV3 planYaml) {
        return getYaml().dump(planYaml);
    }

    @Override
    public SourceCodeParamsYamlV3 to(String s) {
        final SourceCodeParamsYamlV3 p = getYaml().load(s);
        if (p.planYaml ==null) {
            throw new IllegalStateException("#635.010 SourceCode Yaml is null");
        }
        for (ProcessV3 process : p.planYaml.processes) {
            if (process.outputParams==null) {
                process.outputParams = new DataStorageParams(EnumsApi.DataSourcing.launchpad);
            }
        }
        if (p.internalParams==null) {
            p.internalParams = new SourceCodeApiData.PlanInternalParamsYaml();
        }
        return p;
    }


}
