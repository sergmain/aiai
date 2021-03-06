/*
 * Metaheuristic, Copyright (C) 2017-2021, Innovation platforms, LLC
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

package ai.metaheuristic.ai.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.util.SupplierUtil;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Serge
 * Date: 4/10/2020
 * Time: 5:12 PM
 */
public class TestJGrapht {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        public Long id;
        public Long itemId;
        public String code;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            return itemId.equals(item.itemId);
        }

        @Override
        public int hashCode() {
            return itemId.hashCode();
        }
    }

    @Test
    public void test_1() {
        DirectedAcyclicGraph<Item, DefaultEdge> graph = new DirectedAcyclicGraph<>(
                null, SupplierUtil.DEFAULT_EDGE_SUPPLIER, false, true);

        final Item v1001 = new Item(1001L, 1001L, "code-1001");
        graph.addVertex(v1001);

        Item item = graph.vertexSet()
                .stream()
                .filter(o -> o.itemId.equals(1001L))
                .findFirst().orElse(null);
        assertNotNull(item);
        assertEquals(v1001.id, item.id);

        final Item v1002 = new Item(1002L, 1002L, "code-1002");
        graph.addVertex(v1002);

        item = graph.vertexSet()
                .stream()
                .filter(o -> o.itemId.equals(1002L))
                .findFirst().orElse(null);
        assertNotNull(item);
        assertEquals(v1002.id, item.id);

        graph.addEdge(v1001, v1002);
    }

    @Test
    public void testImport() {
        String input="strict digraph G {\n" +
                "  <assembly-raw-file>;\n" +
                "  <dataset-processing>;\n" +
                "  <feature-processing-1>;\n" +
                "  <feature-processing-2>;\n" +
                "  <mh.permute-variables-and-hyper-params>;\n" +
                "  <mh.finish>;\n" +
                "  <assembly-raw-file> -> <dataset-processing>;\n" +
                "  <dataset-processing> -> <feature-processing-1>;\n" +
                "  <dataset-processing> -> <feature-processing-2>;\n" +
                "  <dataset-processing> -> <mh.permute-variables-and-hyper-params>;\n" +
                "  <feature-processing-1> -> <mh.permute-variables-and-hyper-params>;\n" +
                "  <feature-processing-2> -> <mh.permute-variables-and-hyper-params>;\n" +
                "  <mh.permute-variables-and-hyper-params> -> <mh.finish>;\n" +
                "}";
        Graph<String, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
        DOTImporter<String, DefaultEdge> importer = new DOTImporter<>();
        importer.setVertexFactory(id->id);
        importer.importGraph(graph, new StringReader(input));
        System.out.println(graph);
    }

}
