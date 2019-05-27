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
package ai.metaheuristic.ai.core;

import ai.metaheuristic.ai.launchpad.beans.ExperimentSnippet;
import ai.metaheuristic.ai.launchpad.experiment.ExperimentService;
import ai.metaheuristic.commons.CommonConsts;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestSnippetSorting {


    @Test
    public void sort() {
        List<ExperimentSnippet> snippets = new ArrayList<>();
        ExperimentSnippet s1 = new ExperimentSnippet();
        s1.type = CommonConsts.PREDICT_TYPE;
        ExperimentSnippet s2 = new ExperimentSnippet();
        s2.type = CommonConsts.FIT_TYPE;
        Collections.addAll(snippets, s1, s2);
        assertEquals(CommonConsts.PREDICT_TYPE, snippets.get(0).type);
        assertEquals(CommonConsts.FIT_TYPE, snippets.get(1).type);
        ExperimentService.sortSnippetsByType(snippets);
        assertEquals(CommonConsts.FIT_TYPE, snippets.get(0).type);
        assertEquals(CommonConsts.PREDICT_TYPE, snippets.get(1).type);
    }
}