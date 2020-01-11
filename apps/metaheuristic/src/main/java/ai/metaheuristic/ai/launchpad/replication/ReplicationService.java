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
import ai.metaheuristic.ai.launchpad.beans.Snippet;
import ai.metaheuristic.ai.launchpad.data.ReplicationData;
import ai.metaheuristic.ai.launchpad.plan.PlanCache;
import ai.metaheuristic.ai.launchpad.repositories.AccountRepository;
import ai.metaheuristic.ai.launchpad.repositories.CompanyRepository;
import ai.metaheuristic.ai.launchpad.repositories.PlanRepository;
import ai.metaheuristic.ai.launchpad.repositories.SnippetRepository;
import ai.metaheuristic.ai.utils.JsonUtils;
import ai.metaheuristic.ai.utils.RestUtils;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.commons.S;
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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        syncCompanies(assetStateResponse);
        syncAccounts(assetStateResponse);
        syncPLans(assetStateResponse);
    }

    private void syncPLans(ReplicationData.AssetStateResponse assetStateResponse) {
        if (plansInSyncState(assetStateResponse)) {
            return;
        }
    }

    private boolean plansInSyncState(ReplicationData.AssetStateResponse assetStateResponse) {
        return false;
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
        ReplicationData.SnippetAsset snippetAsset = getSnippetAsset(snippetCode);
        if (snippetAsset.isErrorMessages()) {
            log.error("#308.010 Error while getting snippet "+ snippetCode +", error: " + snippetAsset.getErrorMessagesAsStr());
            return;
        }
        Snippet sn = snippetRepository.findByCode(snippetCode);
        if (sn!=null) {
            return;
        }
        snippetAsset.snippet.id=null;
        snippetRepository.save(snippetAsset.snippet);
    }

    private void syncAccounts(ReplicationData.AssetStateResponse assetStateResponse) {
        if (accountsInSyncState(assetStateResponse)) {
            return;
        }
    }

    private boolean accountsInSyncState(ReplicationData.AssetStateResponse assetStateResponse) {
        return false;
    }

    private void syncCompanies(ReplicationData.AssetStateResponse assetStateResponse) {
        if (companiesInSyncState(assetStateResponse)) {
            return;
        }
    }

    private boolean companiesInSyncState(ReplicationData.AssetStateResponse assetStateResponse) {
        return false;
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
        ReplicationData.AssetStateResponse response = (ReplicationData.AssetStateResponse)getData(
                "/rest/v1/replication/current-assets", ReplicationData.AssetStateResponse.class,
                (uri) -> Request.Get(uri).connectTimeout(5000).socketTimeout(20000)
        );
        return response;
    }

    private ReplicationData.SnippetAsset getSnippetAsset(String snippetCode) {
        ReplicationData.SnippetAsset response = (ReplicationData.SnippetAsset)getData(
                "/rest/v1/replication/snippet", ReplicationData.SnippetAsset.class,
                (uri) -> Request.Post(uri)
                        .bodyForm(Form.form().add("snippetCode", snippetCode).build(), StandardCharsets.UTF_8)
                        .connectTimeout(5000)
                        .socketTimeout(20000)
        );
        return response;
    }

    private Object getData(String uri, Class clazz, Function<URI, Request> requestFunc) {
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
                return new ReplicationData.AssetStateResponse( S.f("Error while accessing url %s, http status code: %d",
                        globals.assetSourceUrl, httpResponse.getStatusLine().getStatusCode()));
            }
            final HttpEntity entity = httpResponse.getEntity();
            Object assetResponse = null;
            if (entity != null) {
                assetResponse = JsonUtils.getMapper().readValue(entity.getContent(), clazz);
            }
            return assetResponse;
        }
        catch (SocketTimeoutException th) {
            log.error("Error: {}", th.getMessage());
            return new ReplicationData.AssetStateResponse( S.f("Error while accessing url %s, error message: %s",
                    globals.assetSourceUrl, th.getMessage()));
        }
        catch (Throwable th) {
            log.error("Error", th);
            return new ReplicationData.AssetStateResponse( S.f("Error while accessing url %s, error message: %s",
                    globals.assetSourceUrl, th.getMessage()));
        }

    }
}