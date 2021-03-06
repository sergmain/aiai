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
package ai.metaheuristic.ai.yaml.processor_status;

import ai.metaheuristic.commons.S;
import ai.metaheuristic.commons.exceptions.BlankYamlParamsException;
import ai.metaheuristic.commons.yaml.YamlUtils;
import ai.metaheuristic.commons.yaml.versioning.AbstractParamsYamlUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessorStatusYamlUtilsV1
        extends AbstractParamsYamlUtils<ProcessorStatusYamlV1, ProcessorStatusYaml, Void, Void, Void, Void> {

    @Override
    public int getVersion() {
        return 1;
    }

    @NonNull
    @Override
    public Yaml getYaml() {
        return YamlUtils.init(ProcessorStatusYamlV1.class);
    }

    @NonNull
    @Override
    public ProcessorStatusYaml upgradeTo(@NonNull ProcessorStatusYamlV1 src) {
        src.checkIntegrity();
        ProcessorStatusYaml trg = new ProcessorStatusYaml();
        trg.downloadStatuses = src.downloadStatuses.stream()
                .map( source -> new ProcessorStatusYaml.DownloadStatus(source.functionState,source.functionCode))
                .collect(Collectors.toList());
        if (src.errors!=null) {
            trg.errors = new ArrayList<>(src.errors);
        }
        if (src.env!=null) {
            trg.env = new ProcessorStatusYaml.Env();
            trg.env.tags = src.env.tags;

            if (!src.env.envs.isEmpty()) {
                final Map<String, String> envMap = src.env.envs.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, HashMap::new));
                trg.env.envs.putAll(envMap);
            }

            if (!src.env.disk.isEmpty()) {
                trg.env.disk.addAll(src.env.disk.stream()
                        .map(d -> new ProcessorStatusYaml.DiskStorage(d.code, d.path))
                        .collect(Collectors.toList()));
            }
            if (!src.env.mirrors.isEmpty()) {
                final Map<String, String> mirrorMap = src.env.mirrors.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, HashMap::new));
                trg.env.mirrors.putAll(mirrorMap);
            }
        }
        if (src.log!=null) {
            trg.log = new ProcessorStatusYaml.Log(src.log.logRequested, src.log.requestedOn, src.log.logReceivedOn);
        }
        BeanUtils.copyProperties(src, trg, "downloadStatuses", "errors");
        trg.checkIntegrity();
        return trg;
    }

    @NonNull
    @Override
    public Void downgradeTo(@NonNull Void yaml) {
        return null;
    }

    @Override
    public Void nextUtil() {
        return null;
    }

    @Override
    public Void prevUtil() {
        return null;
    }

    @Override
    public String toString(@NonNull ProcessorStatusYamlV1 yaml) {
        return getYaml().dump(yaml);
    }

    @NonNull
    @Override
    public ProcessorStatusYamlV1 to(@NonNull String s) {
        if (S.b(s)) {
            throw new BlankYamlParamsException("'yaml' parameter is blank");
        }
        final ProcessorStatusYamlV1 p = getYaml().load(s);
        return p;
    }

}
