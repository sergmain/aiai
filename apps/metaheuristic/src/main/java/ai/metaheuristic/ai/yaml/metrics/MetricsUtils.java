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
package ai.metaheuristic.ai.yaml.metrics;

import ai.metaheuristic.commons.yaml.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;

@Slf4j
public class MetricsUtils {

    public static final Metrics EMPTY_METRICS = new Metrics(Metrics.Status.NotFound, null, null);

    private static Yaml getYaml() {
        return YamlUtils.init(Metrics.class);
    }

    private static Yaml getValueYaml() {
        return YamlUtils.init(MetricValues.class);
    }

    public static String toString(Metrics config) {
        return YamlUtils.toString(config, getYaml());
    }

    public static Metrics to(String s) {
        return (Metrics) YamlUtils.to(s, getYaml());
    }

    public static Metrics to(InputStream is) {
        return (Metrics) YamlUtils.to(is, getYaml());
    }

    public static Metrics to(File file) {
        return (Metrics) YamlUtils.to(file, getYaml());
    }

    public static MetricValues getValues(Metrics metrics) {
        if (metrics==null || metrics.getStatus()!= Metrics.Status.Ok) {
            return null;
        }
        //noinspection UnnecessaryLocalVariable
        MetricValues metricValues = (MetricValues) YamlUtils.to(metrics.metrics, getValueYaml());
        return metricValues;
    }

}
