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

package ai.metaheuristic.ai.dispatcher.replication;

import ai.metaheuristic.ai.Globals;
import ai.metaheuristic.ai.dispatcher.data.ReplicationData;
import ai.metaheuristic.ai.utils.JsonUtils;
import ai.metaheuristic.ai.utils.RestUtils;
import ai.metaheuristic.commons.S;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
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
import java.util.function.Function;

/**
 * @author Serge
 * Date: 1/13/2020
 * Time: 7:13 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("dispatcher")
public class ReplicationCoreService {

    public final Globals globals;

    public ReplicationData.AssetStateResponse getAssetStates() {
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

    private static Executor getExecutor(String dispatcherUrl, String restUsername, String restPassword) {
        HttpHost dispatcherHttpHostWithAuth;
        try {
            dispatcherHttpHostWithAuth = URIUtils.extractHost(new URL(dispatcherUrl).toURI());
        } catch (Throwable th) {
            throw new IllegalArgumentException("Can't build HttpHost for " + dispatcherUrl, th);
        }
        return Executor.newInstance()
                .authPreemptive(dispatcherHttpHostWithAuth)
                .auth(dispatcherHttpHostWithAuth, restUsername, restPassword);
    }

    public ReplicationData.ReplicationAsset getData(String uri, Class clazz, Function<URI, Request> requestFunc) {
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