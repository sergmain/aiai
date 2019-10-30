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

package ai.metaheuristic.ai.launchpad.account;

import ai.metaheuristic.ai.launchpad.beans.Account;
import ai.metaheuristic.ai.launchpad.data.AccountData;
import ai.metaheuristic.ai.launchpad.repositories.AccountRepository;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.OperationStatusRest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Serge
 * Date: 10/30/2019
 * Time: 1:21 AM
 */
@Service
@Profile("launchpad")
@RequiredArgsConstructor
public class AccountService {

    public static final List<String> POSSIBLE_ROLES = List.of("ROLE_SERVER_REST_ACCESS", "ROLE_ADMIN","ROLE_MANAGER","ROLE_OPERATOR","ROLE_BILLING","ROLE_DATA");

    private final AccountRepository accountRepository;
    private final AccountCache accountCache;
    private final PasswordEncoder passwordEncoder;

    public AccountData.AccountsResult getAccounts(Pageable pageable, Long companyId)  {
        AccountData.AccountsResult result = new AccountData.AccountsResult();
        result.accounts = accountRepository.findAll(pageable, companyId);
        return result;
    }

    public OperationStatusRest addAccount(Account account, Long companyId) {

        if (StringUtils.isBlank(account.getUsername()) ||
                StringUtils.isBlank(account.getRoles()) ||
                StringUtils.isBlank(account.getPassword()) ||
                StringUtils.isBlank(account.getPassword2()) ||
                StringUtils.isBlank(account.getPublicName())) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,
                    "#237.010 Username, roles, password, and public name must be not null");
        }
        if (account.getUsername().indexOf('=')!=-1 ) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,
                    "#237.020 Username can't contain '='");
        }
        if (!account.getPassword().equals(account.getPassword2())) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,
                    "#237.030 Both passwords must be equal");
        }

        final Account byUsername = accountRepository.findByUsername(account.getUsername());
        if (byUsername !=null) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,
                    String.format("#237.040 Username '%s' was already used", account.getUsername()));
        }

        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setCreatedOn(System.currentTimeMillis());
        account.setAccountNonExpired(true);
        account.setAccountNonLocked(true);
        account.setCredentialsNonExpired(true);
        account.setEnabled(true);
        account.setCompanyId(companyId);

        accountCache.save(account);
        return OperationStatusRest.OPERATION_STATUS_OK;
    }

    public AccountData.AccountResult getAccount(Long id, Long companyId){
        Account account = accountRepository.findById(id).orElse(null);
        if (account == null || !Objects.equals(account.companyId, companyId)) {
            return new AccountData.AccountResult("#237.050 account wasn't found, accountId: " + id);
        }
        Account acc = (Account) account.clone();
        acc.setPassword(null);
        return new AccountData.AccountResult(acc);
    }

    public OperationStatusRest editFormCommit(Long accountId, String publicName, boolean enabled, Long companyId) {
        Account a = accountRepository.findByIdForUpdate(accountId);
        if (a == null || !Objects.equals(a.companyId, companyId)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,"#237.060 account wasn't found, accountId: " + accountId);
        }
        a.setEnabled(enabled);
        a.setPublicName(publicName);
        accountCache.save(a);
        return new OperationStatusRest(EnumsApi.OperationStatus.OK,"The data of account was changed successfully", null);
    }

    public OperationStatusRest passwordEditFormCommit(Long accountId, String password, String password2, Long companyId) {
        if (StringUtils.isBlank(password) || StringUtils.isBlank(password2)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, "#237.080 Both passwords must be not null");
        }

        if (!password.equals(password2)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, "#237.090 Both passwords must be equal");
        }
        Account a = accountRepository.findByIdForUpdate(accountId);
        if (a == null || !Objects.equals(a.companyId, companyId)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, "#237.100 account wasn't found, accountId: " + accountId);
        }
        a.setPassword(passwordEncoder.encode(password));
        accountCache.save(a);

        return new OperationStatusRest(EnumsApi.OperationStatus.OK,"The password was changed successfully", null);
    }

    // this method is using with angular's rest
    public OperationStatusRest roleFormCommit(Long accountId, String roles, Long companyId) {
        Account account = accountRepository.findByIdForUpdate(accountId);
        if (account == null || !Objects.equals(account.companyId, companyId)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,"#237.110 account wasn't found, accountId: " + accountId);
        }
        String str = Arrays.stream(StringUtils.split(roles, ','))
                .map(String::strip)
                .filter(POSSIBLE_ROLES::contains)
                .collect(Collectors.joining(", "));

        account.setRoles(str);
        accountCache.save(account);
        return new OperationStatusRest(EnumsApi.OperationStatus.OK,"The data of account was changed successfully", null);
    }

    // this method is using with company-accounts
    public OperationStatusRest storeRolesForUserById(Long accountId, int roleId, boolean checkbox, Long companyId) {
        Account account = accountRepository.findByIdForUpdate(accountId);
        if (account == null || !Objects.equals(account.companyId, companyId)) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR,"#237.110 account wasn't found, accountId: " + accountId);
        }
        String role = AccountService.POSSIBLE_ROLES.get(roleId);
        boolean isAccountContainsRole = account.hasRole(role);
        if (isAccountContainsRole && !checkbox){
            account.getRolesAsList().remove(role);
        } else if (!isAccountContainsRole && checkbox) {
            account.getRolesAsList().add(role);
        }

        String roles = String.join(",", account.getRolesAsList());
        account.setRoles(roles);
        accountCache.save(account);
        return new OperationStatusRest(EnumsApi.OperationStatus.OK, "Roles was changed successfully", null);
    }

}

