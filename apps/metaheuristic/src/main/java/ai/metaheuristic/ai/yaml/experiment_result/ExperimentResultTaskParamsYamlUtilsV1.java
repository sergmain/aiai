/*
 * Metaheuristic, Copyright (C) 2017-2020, Innovation platforms, LLC
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

package ai.metaheuristic.ai.yaml.experiment_result;

import ai.metaheuristic.api.data.experiment_result.ExperimentResultTaskParamsYaml;
import ai.metaheuristic.api.data.experiment_result.ExperimentResultTaskParamsYamlV1;
import ai.metaheuristic.commons.yaml.YamlUtils;
import ai.metaheuristic.commons.yaml.versioning.AbstractParamsYamlUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Serge
 * Date: 6/22/2019
 * Time: 11:36 PM
 */
public class ExperimentResultTaskParamsYamlUtilsV1
        extends AbstractParamsYamlUtils<ExperimentResultTaskParamsYamlV1, ExperimentResultTaskParamsYaml, Void, Void, Void, Void> {

    @Override
    public int getVersion() {
        return 1;
    }

    @NonNull
    @Override
    public Yaml getYaml() {
        return YamlUtils.init(ExperimentResultTaskParamsYamlV1.class);
    }

    @NonNull
    @Override
    public ExperimentResultTaskParamsYaml upgradeTo(@NonNull ExperimentResultTaskParamsYamlV1 src, Long ... vars) {
        src.checkIntegrity();

        ExperimentResultTaskParamsYaml trg = new ExperimentResultTaskParamsYaml();
        BeanUtils.copyProperties(src, trg);

        trg.metrics.error = src.metrics.error;
        trg.metrics.status = src.metrics.status;
        trg.metrics.values.putAll(src.metrics.values);

        trg.taskParams = new ExperimentResultTaskParamsYaml.TaskParams(src.taskParams.allInline, src.taskParams.inline);

        trg.checkIntegrity();
        return trg;
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
    public String toString(@NonNull ExperimentResultTaskParamsYamlV1 paramsYaml) {
        return getYaml().dump(paramsYaml);
    }

    @NonNull
    @Override
    public ExperimentResultTaskParamsYamlV1 to(@NonNull String s) {
        final ExperimentResultTaskParamsYamlV1 p = getYaml().load(s);
        return p;
    }

}
