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

package ai.metaheuristic.ai.launchpad.beans;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Serge
 * Date: 10/14/2019
 * Time: 8:21 PM
 */
@Entity
@Table(name = "MH_EVENT")
@Data
public class LaunchpadEvent implements Serializable {

    private static final long serialVersionUID = 6281346638344725952L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    private Integer version;

    @Column(name="CREATED_ON")
    public long createdOn;

    @Column(name = "EVENT")
    public String event;

    @Column(name = "PARAMS")
    public String params;

}