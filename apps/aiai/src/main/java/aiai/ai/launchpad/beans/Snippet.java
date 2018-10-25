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
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "AIAI_LP_SNIPPET")
@Data
@ToString(exclude = {"code"})
public class Snippet implements Serializable {
    private static final long serialVersionUID = 4066977399166436522L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    private Integer version;

    @Column(name = "NAME")
    public String name;

    @Column(name = "SNIPPET_TYPE")
    public String type;

    @Column(name = "SNIPPET_VERSION")
    public String snippetVersion;

    @Column(name = "FILENAME")
    public String filename;

    @Column(name = "PARAMS")
    public String params;

    /**
     * programm code, i.e. pyphon, js,...
     */
    @Lob
    @Type(type="org.hibernate.type.BinaryType")
    @Column(name = "CODE")
    @Basic(fetch = FetchType.LAZY)
    // there is some problem with @Basic(fetch = FetchType.LAZY) -
    // it doesn't work at all. So we'll continue to use SnippetBase
    public byte[] code;

    @Column(name = "CHECKSUM")
    public String checksum;

    @Column(name = "ENV")
    public String env;

    @Column(name = "IS_SIGNED")
    public boolean isSigned;

    @Column(name = "CODE_LENGTH")
    public int codeLength;

    public String getSnippetCode() {
        return ""+ name + ':' + snippetVersion;
    }
}