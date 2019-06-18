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

package ai.metaheuristic.api.v1.data.plan;

import ai.metaheuristic.api.v1.data.BaseParams;
import ai.metaheuristic.api.v1.data.Meta;
import ai.metaheuristic.api.v1.launchpad.process.Process;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Serge
 * Date: 6/17/2019
 * Time: 9:01 PM
 */
@Data
public class PlanParamsYaml implements BaseParams {
    @Data
    public static class PlanYaml {
        public List<Process> processes = new ArrayList<>();
        public boolean clean = false;
        public List<Meta> metas;

        public Meta getMeta(String key) {
            if (metas==null) {
                return null;
            }
            for (Meta meta : metas) {
                if (meta.key.equals(key)) {
                    return meta;
                }
            }
            return null;
        }
    }

    public final int version=3;
    public PlanYaml planYaml;
    public PlanApiData.PlanInternalParamsYaml internalParams;
}
