/*
 AiAi, Copyright (C) 2017 - 2018, Serge Maslyukov

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */
package aiai.ai;

import aiai.ai.launchpad.LaunchpadService;
import aiai.ai.station.ArtifactCleaner;
import aiai.ai.station.LaunchpadRequester;
import aiai.ai.station.TaskProcessor;
import aiai.ai.station.TaskAssigner;
import aiai.ai.station.actors.DownloadResourceActor;
import aiai.ai.station.actors.DownloadSnippetActor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@Slf4j
public class Schedulers {

    private final Globals globals;

    private final LaunchpadService launchpadService;

    private final LaunchpadRequester launchpadRequester;
    private final TaskAssigner taskAssigner;
    private final TaskProcessor taskProcessor;
    private final DownloadSnippetActor downloadSnippetActor;
    private final DownloadResourceActor downloadResourceActor;
    private final ArtifactCleaner artifactCleaner;

    public Schedulers(Globals globals, LaunchpadService launchpadService, LaunchpadRequester launchpadRequester, TaskAssigner taskAssigner, TaskProcessor taskProcessor, DownloadSnippetActor downloadSnippetActor, DownloadResourceActor downloadResourceActor, ArtifactCleaner artifactCleaner) {
        this.globals = globals;
        this.launchpadService = launchpadService;
        this.launchpadRequester = launchpadRequester;
        this.taskAssigner = taskAssigner;
        this.taskProcessor = taskProcessor;
        this.downloadSnippetActor = downloadSnippetActor;
        this.downloadResourceActor = downloadResourceActor;
        this.artifactCleaner = artifactCleaner;
    }

    // Launchpad schedulers

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.launchpad.timeout.create-sequence'), 10, 20, 10)*1000 }")
    public void experimentService() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isLaunchpadEnabled) {
            return;
        }
        log.info("ExperimentService.fixedDelayTaskProducer()");
        launchpadService.getExperimentService().fixedDelayTaskProducer();
    }

    // Station schedulers

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.station.timeout.request-launchpad'), 3, 20, 10)*1000 }")
    public void launchRequester() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isStationEnabled) {
            return;
        }
        log.info("LaunchpadRequester.fixedDelay()");
        launchpadRequester.fixedDelay();
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.station.timeout.task-assigner'), 3, 20, 10)*1000 }")
    public void taskAssigner() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isStationEnabled) {
            return;
        }
        log.info("TaskAssigner.fixedDelay()");
        taskAssigner.fixedDelay();
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.station.timeout.task-processor'), 3, 20, 10)*1000 }")
    public void taskProcessor() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isStationEnabled) {
            return;
        }
        log.info("SequenceProcessor.fixedDelay()");
        taskProcessor.fixedDelay();
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.station.timeout.download-snippet'), 3, 20, 10)*1000 }")
    public void downloadSnippetActor() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isStationEnabled) {
            return;
        }
        log.info("DownloadSnippetActor.fixedDelay()");
        downloadSnippetActor.fixedDelay();
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.station.timeout.download-resource'), 3, 20, 10)*1000 }")
    public void downloadResourceActor() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isStationEnabled) {
            return;
        }
        log.info("DownloadSnippetActor.fixedDelay()");
        downloadResourceActor.fixedDelay();
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "#{ T(aiai.ai.utils.EnvProperty).minMax( environment.getProperty('aiai.station.timeout.artifact-cleaner'), 10, 60, 30)*1000 }")
    public void artifactCleaner() {
        if (globals.isUnitTesting) {
            return;
        }
        if (!globals.isStationEnabled) {
            return;
        }
        log.info("ArtifactCleaner.fixedDelay()");
        artifactCleaner.fixedDelay();
    }
}
