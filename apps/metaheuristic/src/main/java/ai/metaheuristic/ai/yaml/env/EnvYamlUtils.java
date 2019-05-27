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
package ai.metaheuristic.ai.yaml.env;

import ai.metaheuristic.commons.yaml.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;

@Slf4j
public class EnvYamlUtils {

    private static Yaml getYaml() {
        return YamlUtils.init(EnvYaml.class);
    }

    public static String toString(EnvYaml config) {
        return YamlUtils.toString(config, getYaml());
    }

    public static EnvYaml to(String s) {
        return (EnvYaml) YamlUtils.to(s, getYaml());
    }

    public static EnvYaml to(InputStream is) {
        return (EnvYaml) YamlUtils.to(is, getYaml());
    }

    public static EnvYaml to(File file) {
        return (EnvYaml) YamlUtils.to(file, getYaml());
    }

}