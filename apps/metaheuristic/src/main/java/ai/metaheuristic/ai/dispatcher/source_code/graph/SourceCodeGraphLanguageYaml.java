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

package ai.metaheuristic.ai.mh.dispatcher..source_code.graph;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.exceptions.SourceCodeGraphException;
import ai.metaheuristic.ai.mh.dispatcher..data.SourceCodeData;
import ai.metaheuristic.ai.utils.CollectionUtils;
import ai.metaheuristic.ai.yaml.source_code.SourceCodeParamsYamlUtils;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.source_code.SourceCodeParamsYaml;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * @author Serge
 * Date: 2/14/2020
 * Time: 10:49 PM
 */
public class SourceCodeGraphLanguageYaml implements SourceCodeGraphLanguage {

    @Override
    public SourceCodeData.SourceCodeGraph parse(String sourceCode, Supplier<String> contextIdSupplier) {

        SourceCodeParamsYaml sourceCodeParams = SourceCodeParamsYamlUtils.BASE_YAML_UTILS.to(sourceCode);
        if (CollectionUtils.isEmpty(sourceCodeParams.source.processes)) {
            throw new SourceCodeGraphException("(CollectionUtils.isEmpty(sourceCodeParams.source.processes))");
        }

        SourceCodeData.SourceCodeGraph scg = new SourceCodeData.SourceCodeGraph();
        scg.clean = sourceCodeParams.source.clean;


        AtomicLong taskId = new AtomicLong();
        long internalContextId = 1L;
        List<Long> parentIds =  new ArrayList<>();

        String currentInternalContextId = "" + internalContextId;
        boolean finishPresent = false;
        Set<String> processCodes = new HashSet<>();
        for (SourceCodeParamsYaml.Process p : sourceCodeParams.source.processes) {
            if (finishPresent) {
                throw new SourceCodeGraphException("(finishPresent)");
            }
            checkProcessCode(processCodes, p);

            SourceCodeData.SimpleTaskVertex v = toVertex(contextIdSupplier, taskId, currentInternalContextId, p);
            SourceCodeGraphUtils.addNewTasksToGraph(scg, v, parentIds);
            if (Consts.MH_FINISH_FUNCTION.equals(v.function.code)) {
                finishPresent = true;
            }

            parentIds.clear();
            parentIds.add(v.taskId);

            SourceCodeParamsYaml.SubProcesses subProcesses = p.subProcesses;
            // tasks for sub-processes of internal function will be produced at runtime phase
            if (subProcesses !=null && p.function.context!= EnumsApi.FunctionExecContext.internal) {
                if (CollectionUtils.isEmpty(subProcesses.processes)) {
                    throw new SourceCodeGraphException("(subProcesses !=null) && (CollectionUtils.isEmpty(subProcesses.processes))");
                }
                List<Long> prevIds = new ArrayList<>();
                prevIds.add(v.taskId);
                String subInternalContextId = null;
                if (subProcesses.logic == EnumsApi.SourceCodeSubProcessLogic.sequential) {
                    subInternalContextId = currentInternalContextId + ',' + contextIdSupplier.get();
                }
                List<Long> andIds = new ArrayList<>();
                for (SourceCodeParamsYaml.Process subP : subProcesses.processes) {
                    checkProcessCode(processCodes, subP);

                    if (subProcesses.logic == EnumsApi.SourceCodeSubProcessLogic.and || subProcesses.logic == EnumsApi.SourceCodeSubProcessLogic.or) {
                        subInternalContextId = currentInternalContextId + ',' + contextIdSupplier.get();
                    }
                    if (subInternalContextId==null) {
                        throw new IllegalStateException("(subInternalContextId==null)");
                    }
                    SourceCodeData.SimpleTaskVertex subV = toVertex(contextIdSupplier, taskId, subInternalContextId, subP);
                    SourceCodeGraphUtils.addNewTasksToGraph(scg, subV, prevIds);
                    if (subProcesses.logic == EnumsApi.SourceCodeSubProcessLogic.sequential) {
                        prevIds.clear();
                        prevIds.add(subV.taskId);
                    }
                    else if (subProcesses.logic == EnumsApi.SourceCodeSubProcessLogic.and) {
                        andIds.add(subV.taskId);
                    }
                    else {
                        throw new NotImplementedException("not yet");
                    }
                }
                parentIds.addAll(andIds);
                parentIds.addAll(prevIds);
            }
        }
        if (!finishPresent) {
            SourceCodeData.SimpleTaskVertex finishVertex = createFinishVertex(contextIdSupplier, taskId, currentInternalContextId);
            SourceCodeGraphUtils.addNewTasksToGraph(scg, finishVertex, parentIds);
        }
        return scg;
    }

    public void checkProcessCode(Set<String> processCodes, SourceCodeParamsYaml.Process p) {
        if (processCodes.contains(p.code)) {
            throw new SourceCodeGraphException("(processCodes.contains(p.code))");
        }
        processCodes.add(p.code);
    }

    private SourceCodeData.SimpleTaskVertex createFinishVertex(Supplier<String> contextIdSupplier, AtomicLong taskId, String currentInternalContextId) {
        SourceCodeData.SimpleTaskVertex v = new SourceCodeData.SimpleTaskVertex();
        v.function = new SourceCodeParamsYaml.FunctionDefForSourceCode(Consts.MH_FINISH_FUNCTION);
        v.taskId = taskId.incrementAndGet();
        v.execContextId = contextIdSupplier.get();
        v.internalContextId = currentInternalContextId;

        v.processName = Consts.MH_FINISH_FUNCTION;
        v.processCode = Consts.MH_FINISH_FUNCTION;
        return v;
    }

    private SourceCodeData.SimpleTaskVertex toVertex(Supplier<String> contextIdSupplier, AtomicLong taskId, String currentInternalContextId, SourceCodeParamsYaml.Process p) {
        SourceCodeData.SimpleTaskVertex v = new SourceCodeData.SimpleTaskVertex();
        v.function = p.function;
        v.preFunctions = p.preFunctions;
        v.postFunctions = p.postFunctions;
        v.taskId = taskId.incrementAndGet();

        v.execContextId = contextIdSupplier.get();
        v.internalContextId = currentInternalContextId;

        v.processName = p.name;
        v.processCode = p.code;
        v.timeoutBeforeTerminate = p.timeoutBeforeTerminate;
        p.input.stream().map(o->o.name).forEach(v.input::add);
        p.output.stream().map(o->o.name).forEach(v.output::add);
        v.metas = p.metas;
        return v;
    }

}
