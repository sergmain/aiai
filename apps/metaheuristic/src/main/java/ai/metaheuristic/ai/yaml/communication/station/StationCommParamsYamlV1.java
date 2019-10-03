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

package ai.metaheuristic.ai.yaml.communication.station;

import ai.metaheuristic.ai.Enums;
import ai.metaheuristic.ai.station.sourcing.git.GitSourcingService;
import ai.metaheuristic.ai.yaml.env.EnvYaml;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.BaseParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Serge
 * Date: 8/29/2019
 * Time: 6:00 PM
 */
@Data
@NoArgsConstructor
public class StationCommParamsYamlV1 implements BaseParams {

    @Override
    public boolean checkIntegrity() {
/*
        final boolean b = planYaml != null && planYaml.planCode != null && !planYaml.planCode.isBlank() &&
                planYaml.processes != null;
        if (!b) {
            throw new IllegalArgumentException(
                    "(boolean b = planYaml != null && planYaml.planCode != null && " +
                            "!planYaml.planCode.isBlank() && planYaml.processes != null) ");
        }
        for (ProcessV5 process : planYaml.processes) {
            if (process.snippets == null || process.snippets.size() == 0) {
                throw new IllegalArgumentException("(process.snippets==null || process.snippets.size()==0) ");
            }
        }
*/

        return true;
    }

    public StationCommContextV1 stationCommContext;
    public RequestStationIdV1 requestStationId;
    public ReportStationStatusV1 reportStationStatus;
    public ReportStationTaskStatusV1 reportStationTaskStatus;
    public RequestTaskV1 requestTask;
    public ReportTaskProcessingResultV1 reportTaskProcessingResult;
    public CheckForMissingOutputResourcesV1 checkForMissingOutputResources;
    public ResendTaskOutputResourceResultV1 resendTaskOutputResourceResult;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationCommContextV1 {
        public String stationId;
        public String sessionId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RequestStationIdV1 {
        public boolean keep = true;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckForMissingOutputResourcesV1 {
        public boolean keep = true;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RequestTaskV1 {
        public boolean acceptOnlySigned;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportStationStatusV1 {
        public EnvYaml env;
        public GitSourcingService.GitStatusInfo gitStatusInfo;
        public String schedule;
        public String sessionId;

        // TODO 2019-05-28, a multi-time-zoned deployment isn't supported right now
        // it'll work but in some cases behaviour can be different
        // need to change it to UTC, Coordinated Universal Time
        public long sessionCreatedOn;
        public String ip;
        public String host;

        // contains text of error which can occur while preparing a station status
        public List<String> errors = null;
        public boolean logDownloadable;
        public int taskParamsVersion;

        public EnumsApi.OS os;

        public void addError(String error) {
            if (errors==null) {
                errors = new ArrayList<>();
            }
            errors.add(error);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReportTaskProcessingResultV1 {

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SimpleTaskExecResult {
            public long taskId;
            public String result;
            public String metrics;
        }

        public List<ReportTaskProcessingResultV1.SimpleTaskExecResult> results = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReportStationTaskStatusV1 {

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SimpleStatus {
            public long taskId;
        }

        public List<ReportStationTaskStatusV1.SimpleStatus> statuses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResendTaskOutputResourceResultV1 {

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SimpleStatus {
            public long taskId;
            public Enums.ResendTaskOutputResourceStatus status;
        }

        public List<ResendTaskOutputResourceResultV1.SimpleStatus> statuses;
    }

    public final int version=1;
}
