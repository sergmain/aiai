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

package aiai.ai.repositories;

import aiai.ai.Enums;
import aiai.ai.beans.ExperimentFeature;
import aiai.ai.beans.ExperimentSnippet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public interface ExperimentFeatureRepository extends CrudRepository<ExperimentFeature, Long> {

    @Transactional(readOnly = true)
    List<ExperimentFeature> findByExperimentId(Long experimentId);

    // continue process the same feature
    @Transactional(readOnly = true)
    List<ExperimentFeature> findAllByIsFinishedIsFalseAndIsInProgressIsTrue();

    // continue process the same feature
    @Transactional(readOnly = true)
    ExperimentFeature findTop1ByIsFinishedIsFalseAndIsInProgressIsTrue();

    // continue process the same feature
    @Transactional(readOnly = true)
    ExperimentFeature findTop1ByIsFinishedIsFalseAndIsInProgressIsTrueAndExperimentId(long experimentId);

    // find new feature for processing
    @Transactional(readOnly = true)
    ExperimentFeature findTop1ByIsFinishedIsFalseAndIsInProgressIsFalse();

    @Transactional(readOnly = true)
    @Query("SELECT f FROM ExperimentFeature f, Experiment e where f.experimentId=e.id and e.isLaunched=true and f.isFinished=false ")
    List<ExperimentFeature> findAllForLaunchedExperimentsAndNotFinishedFeatures();

    @Transactional(readOnly = true)
    @Query("SELECT f FROM ExperimentFeature f, Experiment e where f.experimentId=e.id and e.isLaunched=true and e.execState=:state")
    List<ExperimentFeature> findAllForLaunchedExperiments(int state);


    @Transactional
    void deleteByExperimentId(long experimentId);

}
