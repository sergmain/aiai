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
package ai.metaheuristic.ai.yaml.task_params_new;

import ai.metaheuristic.api.v1.data.SnippetApiData;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class TaskParamNewYaml {
    public Map<String, List<TaskResource>> inputResourceCodes = new LinkedHashMap<>();
    public SnippetApiData.SnippetConfig snippet;
    public Map<String, String> hyperParams;
    public TaskResource outputResourceCode;
    public String workingPath;
    public boolean clean = false;

    @Override
    public String toString() {
        return "TaskParamNewYaml{" +
                "inputResourceCodes=" + inputResourceCodes +
                ", snippet=" + snippet +
                ", hyperParams=" + hyperParams +
                ", outputResourceCode=" + outputResourceCode +
                ", workingPath='" + workingPath + '\'' +
                ", clean=" + clean +
                '}';
    }
}