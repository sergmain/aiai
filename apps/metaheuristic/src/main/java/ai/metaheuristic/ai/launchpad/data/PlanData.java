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

package ai.metaheuristic.ai.launchpad.data;

import ai.metaheuristic.api.data.BaseDataClass;
import ai.metaheuristic.api.data.Meta;
import ai.metaheuristic.api.data.plan.PlanParamsYaml;
import ai.metaheuristic.api.data.workbook.WorkbookParamsYaml;
import ai.metaheuristic.api.launchpad.Plan;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Serge
 * Date: 1/18/2020
 * Time: 4:41 PM
 */
public class PlanData {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PlansForCompany extends BaseDataClass {
        public List<Plan> items;
    }

    @Data
    public static class SimpleTaskVertex {
        public String contextId;

        public String name;
        public String code;
        public PlanParamsYaml.SnippetDefForPlan snippet;
        public List<PlanParamsYaml.SnippetDefForPlan> preSnippets;
        public List<PlanParamsYaml.SnippetDefForPlan> postSnippets;

        /**
         * Timeout before terminating a process with snippet
         * value in seconds
         * null or 0 mean the infinite execution
         */
        public Long timeoutBeforeTerminate;
        public final List<PlanParamsYaml.Variable> input = new ArrayList<>();
        public final List<PlanParamsYaml.Variable> output = new ArrayList<>();
        public List<Meta> metas = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(exclude = "graph")
    public static class SourceCode {
        public boolean clean;
        public final DirectedAcyclicGraph<SimpleTaskVertex, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
    }
}
