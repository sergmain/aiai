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

package ai.metaheuristic.api.data.exec_context;

import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.BaseDataClass;
import ai.metaheuristic.api.data.task.TaskApiData;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Serge
 * Date: 4/20/2020
 * Time: 12:04 AM
 */
public class ExecContextApiData {

    @Data
    @NoArgsConstructor
    public static class VariableInfo {
        public Long id;

        @JsonProperty("nm")
        public String name;

        @JsonProperty("ctx")
        public EnumsApi.VariableContext context;

        @JsonProperty("i")
        public boolean inited;

        @JsonProperty("n")
        public boolean nullified;

        public VariableInfo(Long id, String name, EnumsApi.VariableContext context) {
            this.id = id;
            this.name = name;
            this.context = context;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStateInfo {
        @JsonProperty("tId")
        public Long taskId;

        @JsonProperty("ecId")
        public Long execContextId;

        @JsonProperty("tCtxId")
        public String taskContextId;

        @JsonProperty("p")
        public String process;

        @JsonProperty("f")
        public String functionCode;

        @JsonInclude(value= JsonInclude.Include.NON_EMPTY)
        @Nullable
        @JsonProperty("ins")
        public List<VariableInfo> inputs;

        @JsonInclude(value= JsonInclude.Include.NON_EMPTY)
        @Nullable
        @JsonProperty("outs")
        public List<VariableInfo> outputs;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecContextTasksStatesInfo {
        public List<TaskStateInfo> tasks = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ExecContextsResult extends BaseDataClass {
        public Long sourceCodeId;
        public String sourceCodeUid;
        public boolean sourceCodeValid;
        public EnumsApi.SourceCodeType sourceCodeType;
        public Slice<ExecContextsListItem> instances;
    }

    @Data
    @NoArgsConstructor
    public static class StateCell {
        public boolean empty = true;
        public Long taskId;
        public String state;
        // context of function
        public String context;

        @Nullable
        public List<VariableInfo> outs;

        public StateCell(Long taskId, String state, String context, @Nullable List<VariableInfo> outs) {
            this.empty = false;
            this.taskId = taskId;
            this.state = state;
            this.context = context;
            this.outs = outs;
        }
    }

    @Data
    @AllArgsConstructor
    public static class ColumnHeader {
        public String process;
        public String functionCode;
    }

    @Data
    public static class LineWithState {
        public String context;
        public StateCell[] cells;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ExecContextStateResult extends BaseDataClass {
        public Long sourceCodeId;
        public String sourceCodeUid;
        public boolean sourceCodeValid;
        public EnumsApi.SourceCodeType sourceCodeType;
        public ColumnHeader[] header;
        public LineWithState[] lines;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor
    public static class RawExecContextStateResult extends BaseDataClass {
        public Long sourceCodeId;
        public List<ExecContextApiData.TaskStateInfo> infos;
        public List<String> processCodes;
        public EnumsApi.SourceCodeType sourceCodeType;
        public String sourceCodeUid;
        public boolean sourceCodeValid;
        public Map<Long, TaskApiData.TaskState> states;

        public RawExecContextStateResult(String error) {
            addErrorMessage(error);
        }
    }

}
