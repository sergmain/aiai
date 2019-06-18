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

package ai.metaheuristic.ai.yaml.task;

import ai.metaheuristic.ai.yaml.versioning.AbstractParamsYamlUtils;
import ai.metaheuristic.api.v1.data.task.TaskParamsYaml;
import ai.metaheuristic.api.v1.data.task.TaskParamsYamlV2;
import ai.metaheuristic.commons.yaml.YamlUtils;
import org.springframework.beans.BeanUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Serge
 * Date: 6/17/2019
 * Time: 12:10 AM
 */
public class TaskParamsYamlUtilsV2
        extends AbstractParamsYamlUtils<TaskParamsYamlV2, TaskParamsYaml, Void> {

    @Override
    public int getVersion() {
        return 2;
    }

    public Yaml getYaml() {
        return YamlUtils.init(TaskParamsYamlV2.class);
    }

    @Override
    public TaskParamsYaml upgradeTo(TaskParamsYamlV2 yaml) {
        TaskParamsYaml t = new TaskParamsYaml();
        t.taskYaml = new TaskParamsYaml.TaskYaml();
        BeanUtils.copyProperties(yaml.taskYaml, t.taskYaml);

        return t;
    }

    @Override
    public Void nextUtil() {
        return null;
    }

    public String toString(TaskParamsYamlV2 planYaml) {
        return getYaml().dump(planYaml);
    }

    public TaskParamsYamlV2 to(String s) {
        final TaskParamsYamlV2 p = getYaml().load(s);
        return p;
    }

}
