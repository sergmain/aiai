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

package ai.metaheuristic.ai.launchpad.replication;

import ai.metaheuristic.ai.Globals;
import ai.metaheuristic.ai.launchpad.beans.Company;
import ai.metaheuristic.ai.launchpad.beans.PlanImpl;
import ai.metaheuristic.ai.launchpad.beans.Snippet;
import ai.metaheuristic.ai.launchpad.company.CompanyCache;
import ai.metaheuristic.ai.launchpad.data.ReplicationData;
import ai.metaheuristic.ai.launchpad.plan.PlanCache;
import ai.metaheuristic.ai.launchpad.repositories.AccountRepository;
import ai.metaheuristic.ai.launchpad.repositories.CompanyRepository;
import ai.metaheuristic.ai.launchpad.repositories.PlanRepository;
import ai.metaheuristic.ai.launchpad.repositories.SnippetRepository;
import ai.metaheuristic.ai.launchpad.snippet.SnippetCache;
import ai.metaheuristic.ai.utils.JsonUtils;
import ai.metaheuristic.ai.utils.RestUtils;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.commons.S;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Serge
 * Date: 1/9/2020
 * Time: 12:16 AM
 */
@SuppressWarnings("UnnecessaryLocalVariable")
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("launchpad")
public class ReplicationService {

    public final Globals globals;
    public final CompanyRepository companyRepository;
    public final AccountRepository accountRepository;
    public final PlanRepository planRepository;
    public final SnippetRepository snippetRepository;
    public final PlanCache planCache;
    public final SnippetCache snippetCache;
    public final CompanyCache companyCache;

    public void sync() {
        if (globals.assetMode!= EnumsApi.LaunchpadAssetMode.replicated) {
            return;
        }
        ReplicationData.AssetStateResponse assetStateResponse = getAssetStates();
        if (assetStateResponse.isErrorMessages()) {
            log.error("#308.010 Error while getting actual assets: " + assetStateResponse.getErrorMessagesAsStr());
            return;
        }
        syncSnippets(assetStateResponse.snippets);
        syncPlans(assetStateResponse.plans);
        syncCompanies(assetStateResponse.companies);
        syncAccounts(assetStateResponse.usernames);
    }

    @Data
    @AllArgsConstructor
    private static class PlanLoopEntry {
        public ReplicationData.PlanShortAsset planShort;
        public PlanImpl plan;
    }

    @Data
    @AllArgsConstructor
    private static class CompanyLoopEntry {
        public ReplicationData.CompanyShortAsset companyShort;
        public Company company;
    }

    private void syncPlans(List<ReplicationData.PlanShortAsset> actualPlans) {
        List<PlanLoopEntry> forUpdating = new ArrayList<>(actualPlans.size());
        LinkedList<ReplicationData.PlanShortAsset> forCreating = new LinkedList<>(actualPlans);

        List<Long> ids = planRepository.findAllAsIds();
        for (Long id : ids) {
            PlanImpl p = planCache.findById(id);
            if (p==null) {
                continue;
            }

            boolean isDeleted = true;
            for (ReplicationData.PlanShortAsset actualPlan : actualPlans) {
                if (actualPlan.code.equals(p.code)) {
                    isDeleted = false;
                    if (actualPlan.updateOn != p.getPlanParamsYaml().internalParams.updatedOn) {
                        PlanLoopEntry planLoopEntry = new PlanLoopEntry(actualPlan, p);
                        forUpdating.add(planLoopEntry);
                    }
                    break;
                }
            }

            if (isDeleted) {
                planCache.deleteById(id);
            }
            forCreating.removeIf(planShortAsset -> planShortAsset.code.equals(p.code));
        }

        forUpdating.parallelStream().forEach(this::updatePlan);
        forCreating.parallelStream().forEach(this::createPlan);
    }

    private void updatePlan(PlanLoopEntry planLoopEntry) {
        ReplicationData.PlanAsset planAsset = getPlanAsset(planLoopEntry.plan.code);
        if (planAsset == null) {
            return;
        }

        planLoopEntry.plan.setParams( planAsset.plan.getParams() );
        planLoopEntry.plan.locked = planAsset.plan.locked;
        planLoopEntry.plan.valid = planAsset.plan.valid;

        planCache.save(planLoopEntry.plan);
    }

    private void createPlan(ReplicationData.PlanShortAsset planShortAsset) {
        ReplicationData.PlanAsset planAsset = getPlanAsset(planShortAsset.code);
        if (planAsset == null) {
            return;
        }

        PlanImpl p = planRepository.findByCode(planShortAsset.code);
        if (p!=null) {
            return;
        }

        planAsset.plan.id=null;
        planCache.save(planAsset.plan);
    }

    private ReplicationData.PlanAsset getPlanAsset(String planCode) {
        ReplicationData.PlanAsset planAsset = requestPlanAsset(planCode);
        if (planAsset.isErrorMessages()) {
            log.error("#308.020 Error while getting plan "+ planCode +", error: " + planAsset.getErrorMessagesAsStr());
            return null;
        }
        return planAsset;
    }

    private ReplicationData.CompanyAsset getCompanyAsset(Long uniqueId) {
        ReplicationData.CompanyAsset planAsset = requestCompanyAsset(uniqueId);
        if (planAsset.isErrorMessages()) {
            log.error("#308.020 Error while getting company with uniqueId "+ uniqueId +", error: " + planAsset.getErrorMessagesAsStr());
            return null;
        }
        return planAsset;
    }

    private void syncSnippets(List<String> actualSnippets) {
        snippetRepository.findAllSnippetCodes().parallelStream()
                .filter(s->!actualSnippets.contains(s))
                .map(snippetRepository::findByCode)
                .filter(Objects::nonNull)
                .forEach(s->snippetRepository.deleteById(s.id));

        List<String> currSnippets = snippetRepository.findAllSnippetCodes();
        actualSnippets.parallelStream()
                .filter(s->!currSnippets.contains(s))
                .forEach(this::createSnippet);
    }

    private void createSnippet(String snippetCode) {
        ReplicationData.SnippetAsset snippetAsset = requestSnippetAsset(snippetCode);
        if (snippetAsset.isErrorMessages()) {
            log.error("#308.010 Error while getting snippet "+ snippetCode +", error: " + snippetAsset.getErrorMessagesAsStr());
            return;
        }
        Snippet sn = snippetRepository.findByCode(snippetCode);
        if (sn!=null) {
            return;
        }
        snippetAsset.snippet.id=null;
        snippetCache.save(snippetAsset.snippet);
    }

    private void syncAccounts(List<String> assetStateResponse) {
    }

    private void syncCompanies(List<ReplicationData.CompanyShortAsset> actualCompanies) {
        List<CompanyLoopEntry> forUpdating = new ArrayList<>(actualCompanies.size());
        LinkedList<ReplicationData.CompanyShortAsset> forCreating = new LinkedList<>(actualCompanies);

        List<Long> ids = companyRepository.findAllUniqueIds();
        for (Long id : ids) {
            Company c = companyCache.findByUniqueId(id);
            if (c==null) {
                continue;
            }

            boolean isDeleted = true;
            for (ReplicationData.CompanyShortAsset actualCompany : actualCompanies) {
                if (actualCompany.uniqueId.equals(c.uniqueId)) {
                    isDeleted = false;
                    if (actualCompany.updateOn != c.getCompanyParamsYaml().updatedOn) {
                        CompanyLoopEntry companyLoopEntry = new CompanyLoopEntry(actualCompany, c);
                        forUpdating.add(companyLoopEntry);
                    }
                    break;
                }
            }

            if (isDeleted) {
                log.warn("!!! Strange situation - company wasn't found, uniqueId: {}", id);
            }
            forCreating.removeIf(companyShortAsset -> companyShortAsset.uniqueId.equals(c.uniqueId));
        }

        forUpdating.parallelStream().forEach(this::updateCompany);
        forCreating.parallelStream().forEach(this::createCompany);
    }

    private void createCompany(ReplicationData.CompanyShortAsset companyShortAsset) {
        ReplicationData.CompanyAsset companyAsset = getCompanyAsset(companyShortAsset.uniqueId);
        if (companyAsset == null) {
            return;
        }

        Company c = companyRepository.findByUniqueId(companyShortAsset.uniqueId);
        if (c!=null) {
            return;
        }

        companyAsset.company.id=null;
        companyCache.save(companyAsset.company);
    }

    private void updateCompany(CompanyLoopEntry companyLoopEntry) {
        ReplicationData.CompanyAsset companyAsset = getCompanyAsset(companyLoopEntry.company.uniqueId);
        if (companyAsset == null) {
            return;
        }

        companyLoopEntry.company.name = companyAsset.company.name;
        companyLoopEntry.company.setParams( companyAsset.company.getParams() );

        companyCache.save(companyLoopEntry.company);

    }

    private static Executor getExecutor(String launchpadUrl, String restUsername, String restPassword) {
        HttpHost launchpadHttpHostWithAuth;
        try {
            launchpadHttpHostWithAuth = URIUtils.extractHost(new URL(launchpadUrl).toURI());
        } catch (Throwable th) {
            throw new IllegalArgumentException("Can't build HttpHost for " + launchpadUrl, th);
        }
        return Executor.newInstance()
                .authPreemptive(launchpadHttpHostWithAuth)
                .auth(launchpadHttpHostWithAuth, restUsername, restPassword);
    }

    private ReplicationData.AssetStateResponse getAssetStates() {
        ReplicationData.ReplicationAsset data = getData(
                "/rest/v1/replication/current-assets", ReplicationData.AssetStateResponse.class,
                (uri) -> Request.Get(uri).connectTimeout(5000).socketTimeout(20000)
        );
        if (data instanceof ReplicationData.AssetAcquiringError) {
            return new ReplicationData.AssetStateResponse(((ReplicationData.AssetAcquiringError) data).errorMessages);
        }
        ReplicationData.AssetStateResponse response = (ReplicationData.AssetStateResponse) data;
        return response;
    }

    private ReplicationData.SnippetAsset requestSnippetAsset(String snippetCode) {
        ReplicationData.ReplicationAsset data = getData(
                "/rest/v1/replication/snippet", ReplicationData.SnippetAsset.class,
                (uri) -> Request.Post(uri)
                        .bodyForm(Form.form().add("snippetCode", snippetCode).build(), StandardCharsets.UTF_8)
                        .connectTimeout(5000)
                        .socketTimeout(20000)
        );
        if (data instanceof ReplicationData.AssetAcquiringError) {
            return new ReplicationData.SnippetAsset(((ReplicationData.AssetAcquiringError) data).errorMessages);
        }
        ReplicationData.SnippetAsset response = (ReplicationData.SnippetAsset) data;
        return response;
    }

    private ReplicationData.PlanAsset requestPlanAsset(String planCode) {
        Object data = getData(
                "/rest/v1/replication/plan", ReplicationData.PlanAsset.class,
                (uri) -> Request.Post(uri)
                        .bodyForm(Form.form().add("planCode", planCode).build(), StandardCharsets.UTF_8)
                        .connectTimeout(5000)
                        .socketTimeout(20000)
        );
        if (data instanceof ReplicationData.AssetAcquiringError) {
            return new ReplicationData.PlanAsset(((ReplicationData.AssetAcquiringError) data).errorMessages);
        }
        ReplicationData.PlanAsset response = (ReplicationData.PlanAsset) data;
        return response;
    }

    private ReplicationData.CompanyAsset requestCompanyAsset(Long uniqueId) {
        Object data = getData(
                "/rest/v1/replication/company", ReplicationData.CompanyAsset.class,
                (uri) -> Request.Post(uri)
                        .bodyForm(Form.form().add("uniqueId", uniqueId.toString()).build(), StandardCharsets.UTF_8)
                        .connectTimeout(5000)
                        .socketTimeout(20000)
        );
        if (data instanceof ReplicationData.AssetAcquiringError) {
            return new ReplicationData.CompanyAsset(((ReplicationData.AssetAcquiringError) data).errorMessages);
        }
        ReplicationData.CompanyAsset response = (ReplicationData.CompanyAsset) data;
        return response;
    }

    private ReplicationData.ReplicationAsset getData(String uri, Class clazz, Function<URI, Request> requestFunc) {
        try {
            final String url = globals.assetSourceUrl + uri;

            final URIBuilder builder = new URIBuilder(url).setCharset(StandardCharsets.UTF_8);

            final URI build = builder.build();
            final Request request = requestFunc.apply(build);

            RestUtils.addHeaders(request);
            Response response = getExecutor(globals.assetSourceUrl, globals.assetUsername, globals.assetPassword)
                    .execute(request);

            final HttpResponse httpResponse = response.returnResponse();
            if (httpResponse.getStatusLine().getStatusCode()!=200) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    entity.writeTo(baos);
                }

                log.error("Server response:\n" + baos.toString());
                return new ReplicationData.AssetAcquiringError( S.f("Error while accessing url %s, http status code: %d",
                        globals.assetSourceUrl, httpResponse.getStatusLine().getStatusCode()));
            }
            final HttpEntity entity = httpResponse.getEntity();
            Object assetResponse = null;
            if (entity != null) {
                assetResponse = JsonUtils.getMapper().readValue(entity.getContent(), clazz);
            }
            return (ReplicationData.ReplicationAsset)assetResponse;
        }
        catch (HttpHostConnectException | SocketTimeoutException th) {
            log.error("Error: {}", th.getMessage());
            return new ReplicationData.AssetAcquiringError( S.f("Error while accessing url %s, error message: %s",
                    globals.assetSourceUrl, th.getMessage()));
        }
        catch (Throwable th) {
            log.error("Error", th);
            return new ReplicationData.AssetAcquiringError( S.f("Error while accessing url %s, error message: %s",
                    globals.assetSourceUrl, th.getMessage()));
        }

    }
}
