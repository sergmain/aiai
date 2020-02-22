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

package ai.metaheuristic.ai.dispatcher.internal_functions;

import ai.metaheuristic.api.data.source_code.SourceCodeParamsYaml;

import java.util.List;
import java.util.Map;

/**
 * @author Serge
 * Date: 2/1/2020
 * Time: 9:19 PM
 */
public interface InternalFunction {
    List<InternalFunctionOutput> process(
            Long sourceCodeId, Long execContextId, String internalContextId, SourceCodeParamsYaml.VariableDefinition variableDefinition,
            Map<String, List<String>> inputResourceIds);
}
