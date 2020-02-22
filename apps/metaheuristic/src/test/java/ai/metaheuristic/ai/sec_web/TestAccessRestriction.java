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

package ai.metaheuristic.ai.sec_web;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.sec.SpringSecurityWebAuxTestConfig;
import ai.metaheuristic.ai.yaml.communication.mh.dispatcher..DispatcherCommParamsYaml;
import ai.metaheuristic.ai.yaml.communication.mh.dispatcher..DispatcherCommParamsYamlUtils;
import ai.metaheuristic.ai.yaml.communication.station.StationCommParamsYaml;
import ai.metaheuristic.ai.yaml.communication.station.StationCommParamsYamlUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.Cookie;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author Serge
 * Date: 7/10/2019
 * Time: 1:29 AM
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import({SpringSecurityWebAuxTestConfig.class})
@ActiveProfiles("launchpad")
public class TestAccessRestriction {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void testUnauthorizedAccessToTest() throws Exception {
        mockMvc.perform(get("/rest/v1/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(Consts.SESSIONID_NAME));
    }

    @Test
    @WithUserDetails("data_rest")
    public void testAnonymousAccessToTest() throws Exception {
        mockMvc.perform(get("/rest/v1/test"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(Consts.SESSIONID_NAME));
    }

    @Test
    @WithUserDetails("data_rest")
    public void whenTestAdminCredentials_thenOk() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/v1/test"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(Consts.SESSIONID_NAME)).andReturn();

        Cookie[] cookies = result.getResponse().getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(0, cookies.length);

        mockMvc.perform(get("/rest/v1/test"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(Consts.SESSIONID_NAME));

    }

    @Test
    @WithUserDetails("data_rest")
    public void testSimpleCommunicationWithServer() throws Exception {
        StationCommParamsYaml stationComm = new StationCommParamsYaml();

        final String stationYaml = StationCommParamsYamlUtils.BASE_YAML_UTILS.toString(stationComm);

        MvcResult result = mockMvc.perform(post("/rest/v1/srv-v2/qwe321").contentType(Consts.APPLICATION_JSON_UTF8)
                .content(stationYaml))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(Consts.SESSIONID_NAME)).andReturn();

        String launchpadYaml = result.getResponse().getContentAsString();
        System.out.println("yaml = " + launchpadYaml);

        DispatcherCommParamsYaml d = DispatcherCommParamsYamlUtils.BASE_YAML_UTILS.to(launchpadYaml);


        Assert.assertNotNull(d);
        Assert.assertTrue(d.isSuccess());

    }
}

