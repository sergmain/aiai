/*
 * Metaheuristic, Copyright (C) 2017-2019  Serge Maslyukov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package metaheuristic.api.v1.data;

import metaheuristic.api.v1.data_storage.DataStorageParams;
import metaheuristic.api.v1.launchpad.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskApiData {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class TasksResult extends BaseDataClass {
        public Slice<TaskWIthType> items;
    }

   @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class ListOfTasksResult extends BaseDataClass {
        public List<Task> items;
    }


    @Data
    public static class TaskParamYaml {
        public Map<String, List<String>> inputResourceCodes = new HashMap<>();
        public SnippetApiData.SnippetConfig snippet;
        public SnippetApiData.SnippetConfig preSnippet;
        public SnippetApiData.SnippetConfig postSnippet;
        public Map<String, String> hyperParams;
        public String outputResourceCode;
        public Map<String, DataStorageParams> resourceStorageUrls = new HashMap<>();
        public boolean clean = false;

        /**
         * Timeout before terminate a process with snippet
         * value in seconds
         * null or 0 mean the infinite execution
         */
        public Long timeoutBeforeTerminate;

        // fields which are initialized at station
        public String workingPath;

        // todo this isn't good solution because it doesn't support ftp, hadoop or something else
        // todo as a result we'll support only direct access to files
        public Map<String, List<String>> inputResourceAbsolutePaths = new HashMap<>();
        public String outputResourceAbsolutePath;
    }
}