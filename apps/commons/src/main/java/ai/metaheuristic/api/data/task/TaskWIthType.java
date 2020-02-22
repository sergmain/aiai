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

package ai.metaheuristic.api.data.task;

import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.mh.dispatcher..Task;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Serge
 * Date: 5/9/2019
 * Time: 4:57 PM
 *
 * !!! Must be top level class (not inner) because it's using in SqlQuery in Repository
 */
@Data
@NoArgsConstructor
public class TaskWIthType {
    public Task task;
    public int type;

    public TaskWIthType(Task task, int type) {
        this.task = task;
        this.type = type;
    }

    public String typeAsString() {
        return EnumsApi.ExperimentTaskType.from(type).toString();
    }

}
