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

package ai.metaheuristic.ai.commands;

import ai.metaheuristic.ai.dispatcher.beans.Processor;
import ai.metaheuristic.ai.dispatcher.repositories.ProcessorRepository;
import ai.metaheuristic.ai.dispatcher.southbridge.SouthbridgeService;
import ai.metaheuristic.ai.dispatcher.processor.ProcessorCache;
import ai.metaheuristic.ai.yaml.communication.dispatcher.DispatcherCommParamsYaml;
import ai.metaheuristic.ai.yaml.communication.dispatcher.DispatcherCommParamsYamlUtils;
import ai.metaheuristic.ai.yaml.communication.processor.ProcessorCommParamsYaml;
import ai.metaheuristic.ai.yaml.communication.processor.ProcessorCommParamsYamlUtils;
import ai.metaheuristic.ai.yaml.processor_status.ProcessorStatusYaml;
import ai.metaheuristic.ai.yaml.processor_status.ProcessorStatusYamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Serge
 * Date: 5/19/2019
 * Time: 3:14 AM
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
@ActiveProfiles("dispatcher")
public class TestReAssignProcessorIdTimeoutDifferentSessionId {

    @Autowired
    public SouthbridgeService serverService;

    @Autowired
    public ProcessorCache processorCache;

    @Autowired
    public ProcessorRepository processorRepository;

    private Long processorIdBefore;
    private String sessionIdBefore;
    private long sessionCreatedOn;

    @BeforeEach
    public void before() {

        ProcessorCommParamsYaml processorComm = new ProcessorCommParamsYaml();

        String dispatcherResponse = serverService.processRequest(ProcessorCommParamsYamlUtils.BASE_YAML_UTILS.toString(processorComm), "127.0.0.1");

        DispatcherCommParamsYaml d = DispatcherCommParamsYamlUtils.BASE_YAML_UTILS.to(dispatcherResponse);

        assertNotNull(d);
        assertNotNull(d.getAssignedProcessorId());
        assertNotNull(d.getAssignedProcessorId().getAssignedProcessorId());
        assertNotNull(d.getAssignedProcessorId().getAssignedSessionId());

        processorIdBefore = Long.valueOf(d.getAssignedProcessorId().getAssignedProcessorId());
        sessionIdBefore = d.getAssignedProcessorId().getAssignedSessionId();

        assertTrue(sessionIdBefore.length()>5);

        System.out.println("processorIdBefore: " + processorIdBefore);
        System.out.println("sessionIdBefore: " + sessionIdBefore);

        Long processorId = processorIdBefore;
        Processor s = processorRepository.findByIdForUpdate(processorId);
        assertNotNull(s);

        ProcessorStatusYaml ss = ProcessorStatusYamlUtils.BASE_YAML_UTILS.to(s.status);
        assertNotEquals(0L, ss.sessionCreatedOn);
        assertEquals(sessionIdBefore, ss.sessionId);

        ss.sessionCreatedOn -= (SouthbridgeService.SESSION_TTL + 100000);
        sessionCreatedOn = ss.sessionCreatedOn;
        s.status = ProcessorStatusYamlUtils.BASE_YAML_UTILS.toString(ss);

        Processor s1 = processorCache.save(s);

        ProcessorStatusYaml ss1 = ProcessorStatusYamlUtils.BASE_YAML_UTILS.to(s1.status);
        assertEquals(ss.sessionCreatedOn, ss1.sessionCreatedOn);
    }

    @AfterEach
    public void afterPreparingExperiment() {
        log.info("Start after()");
        if (processorIdBefore !=null) {
            try {
                processorCache.deleteById(processorIdBefore);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    @Test
    public void testReAssignProcessorIdDifferentSessionId() {

        // in this scenario we test that a processor has got a refreshed sessionId

        ProcessorCommParamsYaml processorComm = new ProcessorCommParamsYaml();
        final String newSessionId = sessionIdBefore + '-';
        processorComm.processorCommContext = new ProcessorCommParamsYaml.ProcessorCommContext(processorIdBefore.toString(), newSessionId);

        String dispatcherResponse = serverService.processRequest(ProcessorCommParamsYamlUtils.BASE_YAML_UTILS.toString(processorComm), "127.0.0.1");

        DispatcherCommParamsYaml d = DispatcherCommParamsYamlUtils.BASE_YAML_UTILS.to(dispatcherResponse);


        assertNotNull(d);
        assertNotNull(d.getReAssignedProcessorId());
        assertNotNull(d.getReAssignedProcessorId().getReAssignedProcessorId());
        assertNotNull(d.getReAssignedProcessorId().getSessionId());

        final Long processorId = Long.valueOf(d.getReAssignedProcessorId().getReAssignedProcessorId());
        assertEquals(processorIdBefore, processorId);
        assertNotEquals(newSessionId, d.getReAssignedProcessorId().getSessionId());

        Processor s = processorCache.findById(processorId);

        assertNotNull(s);
        ProcessorStatusYaml ss = ProcessorStatusYamlUtils.BASE_YAML_UTILS.to(s.status);
        assertNotEquals(0L, ss.sessionCreatedOn);
        assertNotEquals(sessionCreatedOn, ss.sessionCreatedOn);
        assertEquals(d.getReAssignedProcessorId().getSessionId(), ss.sessionId);
        assertTrue(ss.sessionCreatedOn > sessionCreatedOn);
    }
}
