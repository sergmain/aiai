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

package ai.metaheuristic.ai.yaml.metadata;

import ai.metaheuristic.ai.Enums;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.BaseParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Serge
 * Date: 12/29/2020
 * Time: 1:33 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataParamsYamlV2 implements BaseParams {

    public final int version = 2;

    @Override
    public boolean checkIntegrity() {
        return true;
    }

    @Data
    @NoArgsConstructor
    @ToString
    @AllArgsConstructor
    public static class DispatcherInfoV2 {
        public String code;
        public String processorId;
        public String sessionId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusV2 {
        public Enums.FunctionState functionState;
        public String code;
        public String assetUrl;
        public EnumsApi.FunctionSourcing sourcing;
        public boolean verified;
    }

    public final LinkedHashMap<String, DispatcherInfoV2> dispatcher = new LinkedHashMap<>();
    public final List<StatusV2> statuses = new ArrayList<>();
    @Nullable
    public LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
}