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
package aiai.ai.launchpad.beans;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "AIAI_LP_FLOW_INSTANCE")
@Data
public class FlowInstance implements Serializable {
    private static final long serialVersionUID = -8071135733968387755L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @Column(name = "FLOW_ID")
    public Long flowId;

    @Column(name = "IS_COMPLETED")
    public boolean completed;

    @Column(name="COMPLETED_ON")
    public long createdOn;

    @Column(name = "INPUT_POOL_CODE")
    public String inputResourcePoolCode;

}