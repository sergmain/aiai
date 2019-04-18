/*
 * AiAi, Copyright (C) 2017-2019  Serge Maslyukov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package aiai.ai.launchpad.rest;

import aiai.ai.launchpad.beans.Account;
import aiai.ai.launchpad.data.UserData;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;

@RestController
@RequestMapping("/ng")
@Profile("launchpad")
@CrossOrigin
//@CrossOrigin(origins="*", maxAge=3600)
public class AuthRestController {

    // this end-point is used by angular's part Only
    @RequestMapping("/user")
    public UserData user(Principal user) {
        UsernamePasswordAuthenticationToken passwordAuthenticationToken = (UsernamePasswordAuthenticationToken) user;
        Account acc = (Account) passwordAuthenticationToken.getPrincipal();
        Collection<GrantedAuthority> authorities = passwordAuthenticationToken.getAuthorities();
        return new UserData(acc.getLogin(), acc.getPublicName(), authorities);
    }


}