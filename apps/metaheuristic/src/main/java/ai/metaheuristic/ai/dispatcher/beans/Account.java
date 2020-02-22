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

package ai.metaheuristic.ai.dispatcher.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * User: Serg
 * Date: 12.08.13
 * Time: 23:19
 */
@Entity
@Table(name = "MH_ACCOUNT")
@Data
@EqualsAndHashCode(of = {"username", "password"})
public class Account implements UserDetails, Serializable, Cloneable {
    private static final long serialVersionUID = 708692073045562337L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    private Integer version;

    // This field contains a value from MH_COMPANY.UNIQUE_ID, !NOT! from ID field
    @Column(name = "COMPANY_ID")
    public Long companyId;

    @Column(name = "USERNAME")
    public String username;

    @Column(name = "PASSWORD")
    public String password;

    @Column(name="IS_ACC_NOT_EXPIRED")
    public boolean accountNonExpired;

    @Column(name="IS_NOT_LOCKED")
    public boolean accountNonLocked;

    @Column(name="IS_CRED_NOT_EXPIRED")
    public boolean credentialsNonExpired;

    @Column(name="IS_ENABLED")
    public boolean enabled;

    @Column(name="PUBLIC_NAME")
    public String publicName;

    @Column(name="MAIL_ADDRESS")
    public String mailAddress;

    @Column(name="PHONE")
    public String phone;

    @Column(name="CREATED_ON")
    public long createdOn;

    @Column(name="UPDATED_ON")
    public long updatedOn;

    public String roles;

    @Column(name="SECRET_KEY")
    public String secretKey;

    @Column(name="TWO_FA")
    public boolean twoFA;

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Object clone()  {
        Account a = new Account();
        BeanUtils.copyProperties(this, a);
        return a;
    }

    @Transient
    @JsonIgnore
    private String password2;

    //TODO add checks on max length
    @Transient
    @JsonIgnore
    private String phoneAsStr;

    @Transient
    @JsonIgnore
    private List<String> rolesAsList = null;

    @Transient
    @JsonIgnore
    private List<SerializableGrantedAuthority> grantedAuthorities = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerializableGrantedAuthority implements GrantedAuthority {
        private static final long serialVersionUID = 8923383713825441981L;
        public String authority;
    }

    public List<SerializableGrantedAuthority> getAuthorities() {
        initRoles();
        return grantedAuthorities;
    }

    @Transient
    @JsonIgnore
    public boolean hasRole(String role) {
        initRoles();
        return rolesAsList.contains(role);
    }

    @Transient
    @JsonIgnore
    public List<String> getRolesAsList() {
        initRoles();
        return rolesAsList;
    }

    @Transient
    @JsonIgnore
    public void storeNewRole(String role) {
        synchronized (this) {
            this.phoneAsStr = role;
            rolesAsList = null;
            grantedAuthorities.clear();
            initRoles();
        }
    }

    private void initRoles() {
        if (rolesAsList==null) {
            synchronized (this) {
                if (rolesAsList==null) {
                    List<String> list = new ArrayList<>();
                    if (roles!=null) {
                        StringTokenizer st = new StringTokenizer(roles, ",");
                        while (st.hasMoreTokens()) {
                            String role = st.nextToken().trim();
                            list.add(role);
                            grantedAuthorities.add(new SerializableGrantedAuthority(role));
                        }
                    }
                    rolesAsList = list;
                }
            }
        }
    }

    @Transient
    @JsonIgnore
    public String getLogin() {
        return username;
    }

}
