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

package ai.metaheuristic.ai.launchpad.internal_snippet_lib;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.launchpad.internal_snippet_lib.resource_splitter.ResourceSplitterSnippet;
import ai.metaheuristic.api.data.task.TaskParamsYaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * @author Serge
 * Date: 1/17/2020
 * Time: 9:47 PM
 */
@Service
@Slf4j
@Profile("launchpad")
@RequiredArgsConstructor
public class InternalSnippetProcessor {

    public final ResourceSplitterSnippet resourceSplitterSnippet;

    public void process(String snippetCode, Long planId, Long workbookId, TaskParamsYaml taskParamsYaml) {
        switch(snippetCode) {
            case Consts.MH_RESOURCE_SPLITTER_SNIPPET:
                resourceSplitterSnippet.process(planId, workbookId, taskParamsYaml);
        }
    }
}