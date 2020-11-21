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

package ai.metaheuristic.ai.yaml.communication.keep_alive;

import ai.metaheuristic.commons.yaml.YamlUtils;
import ai.metaheuristic.commons.yaml.versioning.AbstractParamsYamlUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Serge
 * Date: 10/03/2019
 * Time: 6:02 PM
 */
public class KeepAliveRequestParamYamlUtilsV1 extends
        AbstractParamsYamlUtils<KeepAliveRequestParamYamlV1, KeepAliveRequestParamYaml, Void, Void, Void, Void> {

    @Override
    public int getVersion() {
        return 1;
    }

    @NonNull
    @Override
    public Yaml getYaml() {
        return YamlUtils.init(KeepAliveRequestParamYamlV1.class);
    }

    @NonNull
    @Override
    public KeepAliveRequestParamYaml upgradeTo(@NonNull KeepAliveRequestParamYamlV1 v1, Long ... vars) {
        KeepAliveRequestParamYaml t = new KeepAliveRequestParamYaml();

        if (v1.processor !=null) {
            t.processor = new KeepAliveRequestParamYaml.ReportProcessor();
            BeanUtils.copyProperties(v1.processor, t.processor);
        }
        t.taskIds = v1.taskIds;
        return t;
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
    public String toString(@NonNull KeepAliveRequestParamYamlV1 yaml) {
        return getYaml().dump(yaml);
    }

    @NonNull
    @Override
    public KeepAliveRequestParamYamlV1 to(@NonNull String s) {
        final KeepAliveRequestParamYamlV1 p = getYaml().load(s);
        return p;
    }

}