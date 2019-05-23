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

import ai.metaheuristic.api.v1.data.TaskApiData;
import ai.metaheuristic.commons.yaml.YamlUtils;
import org.yaml.snakeyaml.Yaml;

public class TaskParamYamlUtils {

    private static Yaml getYaml() {
        return YamlUtils.init(TaskApiData.TaskParamYaml.class);
    }

    public static String toString(TaskApiData.TaskParamYaml taskParamYaml) {
        return getYaml().dump(taskParamYaml);
    }

    public static TaskApiData.TaskParamYaml toTaskYaml(String s) {
        return getYaml().load(s);
    }


}
