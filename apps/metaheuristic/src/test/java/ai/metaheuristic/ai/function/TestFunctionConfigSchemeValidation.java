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

package ai.metaheuristic.ai.function;

import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.sourcing.GitInfo;
import ai.metaheuristic.commons.yaml.function_list.FunctionConfigListYaml;
import ai.metaheuristic.commons.yaml.function_list.FunctionConfigListYamlUtils;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

import static ai.metaheuristic.ai.dispatcher.function.FunctionService.FUNCTION_CONFIG_LIST_YAML_SCHEME_VALIDATOR;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Serge
 * Date: 9/10/2020
 * Time: 12:02 AM
 */
public class TestFunctionConfigSchemeValidation {

    @Test
    public void testOk() {

        String yaml = createYaml();
        String result = FUNCTION_CONFIG_LIST_YAML_SCHEME_VALIDATOR.validateStructureOfDispatcherYaml(yaml);
        assertNull(result, result);
    }

    @Test
    public void testError() {

        String yaml = createYaml();
        final int endIndex = yaml.lastIndexOf("version: 1");
        assertNotEquals(-1, endIndex);
        yaml = yaml.substring(0, endIndex)+ "  aaa: 2\n";
        String result = FUNCTION_CONFIG_LIST_YAML_SCHEME_VALIDATOR.validateStructureOfDispatcherYaml(yaml);
        assertNotNull(result, result);
    }

    @NonNull
    public String createYaml() {
        FunctionConfigListYaml cfgList = new FunctionConfigListYaml();
        FunctionConfigListYaml.FunctionConfig cfg = new FunctionConfigListYaml.FunctionConfig();
        cfg.checksumMap = Map.of(EnumsApi.HashAlgo.SHA256, "123");
        cfg.code = "code";
        cfg.type = "type";
        cfg.file = "file";
        cfg.params = "params";
        cfg.env = "env";
        cfg.sourcing = EnumsApi.FunctionSourcing.dispatcher;
        cfg.git = new GitInfo("repo", "branch", "commit");
        cfg.skipParams = false;
        cfg.metas.add(Map.of("meta-key", "meta-value"));
        cfgList.functions = List.of(cfg);

        return FunctionConfigListYamlUtils.BASE_YAML_UTILS.toString(cfgList);
    }
}