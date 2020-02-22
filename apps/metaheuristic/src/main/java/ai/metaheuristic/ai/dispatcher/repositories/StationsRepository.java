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

package ai.metaheuristic.ai.dispatcher.repositories;

import ai.metaheuristic.ai.dispatcher.beans.Station;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User: Serg
 * Date: 25.06.2017
 * Time: 15:52
 */
@Repository
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Profile("dispatcher")
public interface StationsRepository extends CrudRepository<Station, Long> {

    @Transactional(readOnly = true)
    Optional<Station> findById(Long id);

    @Query(value="select s from Station s where s.id=:id")
    Station findByIdForUpdate(Long id);

    @Transactional(readOnly = true)
    Page<Station> findAll(Pageable pageable);

    @Transactional(readOnly = true)
    @Query(value="select s.id from Station s order by s.updatedOn desc")
    Slice<Long> findAllByOrderByUpdatedOnDescId(Pageable pageable);

}