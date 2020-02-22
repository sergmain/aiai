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

package ai.metaheuristic.ai.yaml.mh.dispatcher._lookup;

import lombok.Data;

@Data
public class ExtendedTimePeriod {

    public enum SchedulePolicy {
        normal, strict
    }

    @Data
    public static class WeekTimePeriod {
        public String mon;
        public String tue;
        public String wed;
        public String thu;
        public String fri;
        public String sat;
        public String sun;
    }

    public String workingDay;
    public String weekend;
    public String dayMask;
    public String holiday;
    public String exceptionWorkingDay;
    public WeekTimePeriod week;
    public SchedulePolicy policy = SchedulePolicy.normal;
}
