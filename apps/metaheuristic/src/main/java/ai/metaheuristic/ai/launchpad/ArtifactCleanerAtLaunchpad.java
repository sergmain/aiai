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
package ai.metaheuristic.ai.launchpad;

import ai.metaheuristic.ai.Globals;
import ai.metaheuristic.ai.launchpad.repositories.WorkbookRepository;
import ai.metaheuristic.ai.launchpad.repositories.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Service
@Slf4j
@Profile("launchpad")
public class ArtifactCleanerAtLaunchpad {

    private final Globals globals;
    private final CleanerTasks cleanerTasks;
    private final WorkbookRepository workbookRepository;

    @Service
    @Profile("launchpad")
    public static class CleanerTasks {
        private final TaskRepository taskRepository;

        public CleanerTasks(TaskRepository taskRepository) {
            this.taskRepository = taskRepository;
        }

        @Transactional
        public int cleanTasks(Set<Long> ids, int page, AtomicBoolean isFound) {
            try (Stream<Object[]> stream = taskRepository.findAllAsTaskSimple(PageRequest.of(page++, 100))) {
                stream
                        .forEach(t -> {
                            isFound.set(true);
                            if (!ids.contains((Long) t[1])) {
                                log.info("Found orphan task #{}, workbookId: #{}", t[0], t[1]);
                                taskRepository.deleteById((Long) t[0]);
                            }
                        });
            }
            return page;
        }
    }

    public ArtifactCleanerAtLaunchpad(Globals globals, WorkbookRepository workbookRepository, CleanerTasks cleanerTasks) {
        this.globals = globals;
        this.workbookRepository = workbookRepository;
        this.cleanerTasks = cleanerTasks;
    }

    public void fixedDelay() {
        // maybe we have to delete this because we already have @Profile("launchpad")
        if (!globals.isLaunchpadEnabled) {
            return;
        }

        Set<Long> ids = new HashSet<>();
        workbookRepository.findAll().forEach( o -> ids.add(o.getId()));

        int page = 0;
        final AtomicBoolean isFound = new AtomicBoolean();
        do {
            isFound.set(false);
            page = cleanerTasks.cleanTasks(ids, page, isFound);
        } while (isFound.get());
    }

}
