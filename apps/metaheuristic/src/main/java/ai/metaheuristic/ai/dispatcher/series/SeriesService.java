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

package ai.metaheuristic.ai.dispatcher.series;

import ai.metaheuristic.ai.dispatcher.DispatcherContext;
import ai.metaheuristic.ai.dispatcher.beans.Series;
import ai.metaheuristic.ai.dispatcher.repositories.SeriesRepository;
import ai.metaheuristic.ai.yaml.series.SeriesParamsYaml;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.OperationStatusRest;
import ai.metaheuristic.commons.S;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Serge
 * Date: 3/30/2021
 * Time: 2:51 PM
 */
@Service
@Slf4j
@Profile("dispatcher")
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;

    @Transactional
    public void addSeriesCommit(String name) {
        Series e = new Series();
        e.name = name;

        SeriesParamsYaml params = new SeriesParamsYaml();
        e.updateParams(params);

        seriesRepository.save(e);
    }

    @Transactional
    public OperationStatusRest deleteSeriesById(Long seriesId, DispatcherContext context) {
        if (seriesId==null) {
            return OperationStatusRest.OPERATION_STATUS_OK;
        }
        seriesRepository.deleteById(seriesId);
        return OperationStatusRest.OPERATION_STATUS_OK;
    }

    @Transactional
    public OperationStatusRest editCommit(Long id, String name, DispatcherContext context) {
        if (id==null) {
            return OperationStatusRest.OPERATION_STATUS_OK;
        }
        if (S.b(name)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, "Name of Series can't be empty");
        }
        Series series = seriesRepository.findById(id).orElse(null);
        if (series==null) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, "Series #"+id+" wasn't found");
        }
        series.name = name.strip();
        seriesRepository.save(series);
        return OperationStatusRest.OPERATION_STATUS_OK;
    }
}