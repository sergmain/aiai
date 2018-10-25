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
package aiai.ai.yaml.station;

import lombok.Data;

@Data
public class StationExperimentSequence {
    public long experimentId;

    public long experimentSequenceId;

    public long createdOn;

    public Long launchedOn;

    public Long finishedOn;

    public Long reportedOn;

    public String params;

    public String metrics;

    // TODO right not there will be 2 the same properties in yaml file - isReported and reported. Won't fix this at this momonet
    public boolean isReported;

    // TODO right not there will be 2 the same properties in yaml file - isDelivered and delivered. Won't fix this at this momonet
    public boolean isDelivered;

    public String snippetExecResults;
}