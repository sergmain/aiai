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
package ai.metaheuristic.ai.yaml.station_status;

import ai.metaheuristic.commons.yaml.YamlUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;

public class StationStatusUtils {

    private static Yaml getYaml() {
        return YamlUtils.init(StationStatus.class);
    }

    public static String toString(StationStatus config) {
        return YamlUtils.toString(config, getYaml());
    }

    public static StationStatus to(String s) {
        return (StationStatus) YamlUtils.to(s, getYaml());
    }

    public static StationStatus to(InputStream is) {
        return (StationStatus) YamlUtils.to(is, getYaml());
    }

    public static StationStatus to(File file) {
        return (StationStatus) YamlUtils.to(file, getYaml());
    }

}