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
package ai.metaheuristic.ai.launchpad.snippet;

import ai.metaheuristic.commons.yaml.snippet.SnippetConfigList;
import ai.metaheuristic.commons.yaml.snippet.SnippetConfigListUtils;
import ai.metaheuristic.commons.yaml.snippet.SnippetConfigUtils;
import ai.metaheuristic.commons.yaml.snippet.SnippetUtils;
import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.Globals;
import ai.metaheuristic.ai.launchpad.beans.ExperimentSnippet;
import ai.metaheuristic.ai.launchpad.beans.Snippet;
import ai.metaheuristic.ai.launchpad.binary_data.BinaryDataService;
import ai.metaheuristic.ai.launchpad.experiment.ExperimentUtils;
import ai.metaheuristic.ai.launchpad.repositories.ExperimentSnippetRepository;
import ai.metaheuristic.ai.launchpad.repositories.SnippetRepository;
import ai.metaheuristic.ai.snippet.SnippetCode;
import ai.metaheuristic.ai.utils.SimpleSelectOption;
import ai.metaheuristic.api.v1.EnumsApi;
import ai.metaheuristic.api.v1.data.SnippetApiData;
import ai.metaheuristic.commons.CommonConsts;
import ai.metaheuristic.commons.utils.Checksum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
@Profile("launchpad")
public class SnippetService {

    private final Globals globals;
    private final SnippetRepository snippetRepository;
    private final SnippetCache snippetCache;
    private final ExperimentSnippetRepository experimentSnippetRepository;
    private final BinaryDataService binaryDataService;

    public SnippetService(Globals globals, SnippetRepository snippetRepository, SnippetCache snippetCache, ExperimentSnippetRepository experimentSnippetRepository, BinaryDataService binaryDataService) {
        this.globals = globals;
        this.snippetRepository = snippetRepository;
        this.snippetCache = snippetCache;
        this.experimentSnippetRepository = experimentSnippetRepository;
        this.binaryDataService = binaryDataService;
    }

    @PostConstruct
    public void init() {
    }

    public List<ExperimentSnippet> getTaskSnippetsForExperiment(Long experimentId) {
        List<ExperimentSnippet> experimentSnippets = experimentSnippetRepository.findByExperimentId(experimentId);
        ExperimentUtils.sortExperimentSnippets(experimentSnippets);
        return experimentSnippets;
    }

    public SnippetApiData.SnippetConfig getSnippetConfig(String snippetCode) {
        SnippetApiData.SnippetConfig snippetConfig = null;
        if(StringUtils.isNotBlank(snippetCode)) {
            Snippet postSnippet = snippetRepository.findByCode(snippetCode);
            if (postSnippet != null) {
                snippetConfig = SnippetConfigUtils.to(postSnippet.params);
            } else {
                log.warn("#295.07 Can't find snippet for code {}", snippetCode);
            }
        }
        return snippetConfig;
    }

    public static void sortSnippetsByType(List<ExperimentSnippet> snippets) {
        snippets.sort(Comparator.comparing(ExperimentSnippet::getType));
    }

    public boolean hasFit(List<ExperimentSnippet> experimentSnippets) {
        if (experimentSnippets ==null || experimentSnippets.isEmpty()) {
            return false;
        }
        for (ExperimentSnippet snippet : experimentSnippets) {
            if (CommonConsts.FIT_TYPE.equals(snippet.getType())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPredict(List<ExperimentSnippet> experimentSnippets) {
        if (experimentSnippets ==null || experimentSnippets.isEmpty()) {
            return false;
        }
        for (ExperimentSnippet snippet : experimentSnippets) {
            if (CommonConsts.PREDICT_TYPE.equals(snippet.getType())) {
                return true;
            }
        }
        return false;
    }

    public interface SnippetFilter {
        boolean filter(Snippet snippet);
    }

    public List<SimpleSelectOption> getSelectOptions(Iterable<Snippet> snippets, List<SnippetCode> snippetCodes,
                                                     SnippetFilter snippetFilter) {
        List<SimpleSelectOption> selectOptions = new ArrayList<>();
        for (Snippet snippet : snippets) {
            boolean isExist=false;
            for (SnippetCode snippetCode : snippetCodes) {
                if (snippet.getCode().equals(snippetCode.getSnippetCode()) ) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                if (snippetFilter.filter(snippet)) {
                    continue;
                }
                selectOptions.add( new SimpleSelectOption(snippet.getCode(), String.format("Type: %s; Code: %s", snippet.getType(), snippet.getCode())));
            }
        }
        return selectOptions;
    }

    void loadSnippetsRecursively(List<SnippetApiData.SnippetConfigStatus> statuses, File startDir) throws IOException {
        final File[] dirs = startDir.listFiles(File::isDirectory);

        if (dirs!=null) {
            for (File dir : dirs) {
                log.info("Load snippets from {}", dir.getPath());
                statuses.addAll(loadSnippetsFromDir(dir));
                loadSnippetsRecursively(statuses, dir);
            }
        }
    }

    /**
     * load snippets from directory
     *
     * @param srcDir File
     */
    List<SnippetApiData.SnippetConfigStatus> loadSnippetsFromDir(File srcDir) throws IOException {
        File yamlConfigFile = new File(srcDir, "snippets.yaml");
        if (!yamlConfigFile.exists()) {
            log.error("#295.11 File 'snippets.yaml' wasn't found in dir {}", srcDir.getAbsolutePath());
            return Collections.emptyList();
        }

        String cfg = FileUtils.readFileToString(yamlConfigFile, StandardCharsets.UTF_8);
        SnippetConfigList snippetConfigList = SnippetConfigListUtils.to(cfg);
        List<SnippetApiData.SnippetConfigStatus> statuses = new ArrayList<>();
        for (SnippetApiData.SnippetConfig snippetConfig : snippetConfigList.snippets) {
            SnippetApiData.SnippetConfigStatus status = null;
            try {
                status = SnippetUtils.validate(snippetConfig);
                if (!status.isOk) {
                    log.error(status.error);
                    continue;
                }
                String sum=null;
                File file = null;
                if (globals.isSnippetChecksumRequired) {
                    switch(snippetConfig.sourcing) {
                        case launchpad:
                            file = new File(srcDir, snippetConfig.file);
                            if (!file.exists()) {
                                status = new SnippetApiData.SnippetConfigStatus(false,
                                        "#295.14 File " + snippetConfig.file + " wasn't found in dir " + srcDir.getAbsolutePath());
                                continue;
                            }
                            try (InputStream inputStream = new FileInputStream(file)) {
                                sum = Checksum.getChecksum(EnumsApi.Type.SHA256, inputStream);
                            }
                            snippetConfig.info.length = file.length();
                            break;
                        case station:
                        case git:
                            String s = "" + snippetConfig.env+", " + snippetConfig.file +" " + snippetConfig.params;
                            sum = Checksum.getChecksum(EnumsApi.Type.SHA256, new ByteArrayInputStream(s.getBytes()));
                            break;
                    }
                }

                Snippet snippet = snippetRepository.findByCodeForUpdate(snippetConfig.code);
                // there is snippet with the same name:version
                if (snippet!=null) {
                    SnippetApiData.SnippetConfig sc = SnippetConfigUtils.to(snippet.params);

                    // new snippet is to replace one which is already in db
                    if (globals.isReplaceSnapshot && snippetConfig.code.endsWith(Consts.SNAPSHOT_SUFFIX)) {
                        // there isn't any checksum for current snippet in db
                        if (sc.checksum ==null) {
                            storeSnippet(snippetConfig, sum, file, snippet);
                        }
                        else {
                            final String checksum = Checksum.fromJson(sc.checksum).checksums.get(EnumsApi.Type.SHA256);
                            // there checksum for current snippet in db isn't equal to new checksum
                            if (!checksum.equals(sum)) {
                                storeSnippet(snippetConfig, sum, file, snippet);
                            }
                        }
                    }
                    else {
                        status = new SnippetApiData.SnippetConfigStatus(false,
                                "#295.20 Updating of snippets is prohibited, not a snapshot version, '"+snippet.code+"'");
                        //noinspection UnnecessaryContinue
                        continue;
                    }
                }
                else {
                    snippet = new Snippet();
                    storeSnippet(snippetConfig, sum, file, snippet);
                }
            }
            catch(Throwable th) {
                status = new SnippetApiData.SnippetConfigStatus(false,
                        "#295.23 Error "+th.getClass().getName()+" while processing snippet '"+snippetConfig.code+"': "+th.getMessage());
            }
            finally {
                statuses.add(status!=null
                        ? status
                        : new SnippetApiData.SnippetConfigStatus(false,
                        "#295.30 Status of snippet "+snippetConfig.code+" is unknown, this status needs to be investigated"));
            }
        }
        return statuses;
    }

    private void storeSnippet(SnippetApiData.SnippetConfig snippetConfig, String sum, File file, Snippet snippet) throws IOException {
        setChecksum(snippetConfig, sum);
        snippet.code = snippetConfig.code;
        snippet.type = snippetConfig.type;
        snippet.params = SnippetConfigUtils.toString(snippetConfig);
        snippetCache.save(snippet);
        if (file != null) {
            try (InputStream inputStream = new FileInputStream(file)) {
                String snippetCode = snippet.getCode();
                binaryDataService.save(inputStream, snippetConfig.info.length, EnumsApi.BinaryDataType.SNIPPET, snippetCode, snippetCode, false, null, null, null);
            }
        }
    }

    private void setChecksum(SnippetApiData.SnippetConfig snippetConfig, String sum) {
        if (sum==null) {
            snippetConfig.checksum = null;
            snippetConfig.info.setSigned(false);
            return;
        }

        if (snippetConfig.checksumMap != null) {
            // already defined checksum in snippets.yaml
            Checksum checksum = new Checksum();
            checksum.checksums.putAll(snippetConfig.checksumMap);
            snippetConfig.checksum = checksum.toJson();
            boolean isSigned = false;
            for (Map.Entry<EnumsApi.Type, String> entry : snippetConfig.checksumMap.entrySet()) {
                if (entry.getKey().isSign) {
                    isSigned = true;
                    break;
                }
            }
            snippetConfig.info.setSigned(isSigned);
        } else {
            // set the new checksum
            snippetConfig.checksum = new Checksum(EnumsApi.Type.SHA256, sum).toJson();
            snippetConfig.info.setSigned(false);
        }
    }
}