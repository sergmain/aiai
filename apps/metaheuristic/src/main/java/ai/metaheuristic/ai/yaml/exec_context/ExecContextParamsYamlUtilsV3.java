/*
 * Metaheuristic, Copyright (C) 2017-2021, Innovation platforms, LLC
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

package ai.metaheuristic.ai.yaml.exec_context;

import ai.metaheuristic.api.data.exec_context.ExecContextParamsYaml;
import ai.metaheuristic.api.data.exec_context.ExecContextParamsYamlV3;
import ai.metaheuristic.commons.yaml.YamlUtils;
import ai.metaheuristic.commons.yaml.versioning.AbstractParamsYamlUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.util.stream.Collectors;

/**
 * @author Serge
 * Date: 6/10/2021
 * Time: 2:13 PM
 */

public class ExecContextParamsYamlUtilsV3
        extends AbstractParamsYamlUtils<ExecContextParamsYamlV3, ExecContextParamsYaml, Void, Void, Void, Void> {

    @Override
    public int getVersion() {
        return 3;
    }

    @NonNull
    @Override
    public Yaml getYaml() {
        return YamlUtils.init(ExecContextParamsYamlV3.class);
    }

    @NonNull
    @Override
    public ExecContextParamsYaml upgradeTo(@NonNull ExecContextParamsYamlV3 v3) {
        ExecContextParamsYaml t = new ExecContextParamsYaml();

        // right now we don't need to convert Graph because it has only one version of structure
        // so just copying of graph field is Ok
        t.clean = v3.clean;
        t.sourceCodeUid = v3.sourceCodeUid;
        t.processesGraph = v3.processesGraph;
        v3.processes.stream().map(ExecContextParamsYamlUtilsV3::toProcess).collect(Collectors.toCollection(()->t.processes));
        initVariables(v3.variables, t.variables);

        return t;
    }

    private void initVariables(ExecContextParamsYamlV3.VariableDeclarationV3 v3, ExecContextParamsYaml.VariableDeclaration v) {
        v.inline.putAll(v3.inline);
        v.globals = v3.globals;
        v3.inputs.forEach(o->v.inputs.add(toVariable(o)));
        v3.outputs.forEach(o->v.outputs.add(toVariable(o)));
    }

    private static ExecContextParamsYaml.Process toProcess(ExecContextParamsYamlV3.ProcessV3 p2) {
        ExecContextParamsYaml.Process p = new ExecContextParamsYaml.Process();
        BeanUtils.copyProperties(p2, p, "function", "preFunctions", "postFunctions", "inputs", "outputs", "metas");
        p.function = toFunction(p2.function);
        p.preFunctions = p2.preFunctions!=null ? p2.preFunctions.stream().map(ExecContextParamsYamlUtilsV3::toFunction).collect(Collectors.toList()) : null;
        p.postFunctions = p2.postFunctions!=null ? p2.postFunctions.stream().map(ExecContextParamsYamlUtilsV3::toFunction).collect(Collectors.toList()) : null;
        p2.inputs.stream().map(ExecContextParamsYamlUtilsV3::toVariable).collect(Collectors.toCollection(()->p.inputs));
        p2.outputs.stream().map(ExecContextParamsYamlUtilsV3::toVariable).collect(Collectors.toCollection(()->p.outputs));
        p.metas.addAll(p2.metas);
        if (p2.cache!=null) {
            p.cache = new ExecContextParamsYaml.Cache(p2.cache.enabled, p2.cache.omitInline);
        }
        p.tags = p2.tags;
        p.priority = p2.priority;
        p.condition = p2.condition;
        return p;
    }

    private static ExecContextParamsYaml.Variable toVariable(ExecContextParamsYamlV3.VariableV3 v) {
        return new ExecContextParamsYaml.Variable(v.name, v.context, v.sourcing, v.git, v.disk, v.parentContext, v.type, v.getNullable(), v.ext);
    }

    @NonNull
    private static ExecContextParamsYaml.FunctionDefinition toFunction(ExecContextParamsYamlV3.FunctionDefinitionV3 f1) {
        return new ExecContextParamsYaml.FunctionDefinition(f1.code, f1.params, f1.context);
    }

    @NonNull
    @Override
    public Void downgradeTo(@NonNull Void yaml) {
        return null;
    }

    @Override
    public Void nextUtil() {
        return null;
    }

    @Override
    public Void prevUtil() {
        return null;
    }

    @Override
    public String toString(@NonNull ExecContextParamsYamlV3 yaml) {
        return getYaml().dump(yaml);
    }

    @NonNull
    @Override
    public ExecContextParamsYamlV3 to(@NonNull String s) {
        final ExecContextParamsYamlV3 p = getYaml().load(s);
        return p;
    }

}
