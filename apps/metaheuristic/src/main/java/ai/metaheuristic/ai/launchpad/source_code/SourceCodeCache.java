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

package ai.metaheuristic.ai.launchpad.source_code;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.api.launchpad.SourceCode;
import ai.metaheuristic.ai.launchpad.beans.SourceCodeImpl;
import ai.metaheuristic.ai.launchpad.repositories.SourceCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("launchpad")
public class SourceCodeCache {

    private final SourceCodeRepository sourceCodeRepository;

    public SourceCodeCache(SourceCodeRepository sourceCodeRepository) {
        this.sourceCodeRepository = sourceCodeRepository;
    }

    @CacheEvict(value = {Consts.PLANS_CACHE}, key = "#result.id")
    public SourceCodeImpl save(SourceCodeImpl plan) {
        return sourceCodeRepository.saveAndFlush(plan);
    }

    @Cacheable(cacheNames = {Consts.PLANS_CACHE}, unless="#result==null")
    public SourceCodeImpl findById(Long id) {
        return sourceCodeRepository.findById(id).orElse(null);
    }

    @CacheEvict(cacheNames = {Consts.PLANS_CACHE}, key = "#sourceCode.id")
    public void delete(SourceCode sourceCode) {
        if (sourceCode ==null || sourceCode.getId()==null) {
            return;
        }
        try {
            sourceCodeRepository.deleteById(sourceCode.getId());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Error", e);
        }
    }

    @CacheEvict(cacheNames = {Consts.PLANS_CACHE}, key = "#id")
    public void deleteById(Long id) {
        if (id==null) {
            return;
        }
        try {
            sourceCodeRepository.deleteById(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Error", e);
        }
    }
}