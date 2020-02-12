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

package ai.metaheuristic.ai.launchpad.rest.v1;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.exceptions.BinaryDataNotFoundException;
import ai.metaheuristic.ai.launchpad.LaunchpadContext;
import ai.metaheuristic.ai.launchpad.batch.BatchTopLevelService;
import ai.metaheuristic.ai.launchpad.context.LaunchpadContextService;
import ai.metaheuristic.ai.launchpad.data.BatchData;
import ai.metaheuristic.ai.launchpad.data.SourceCodeData;
import ai.metaheuristic.ai.launchpad.source_code.SourceCodeService;
import ai.metaheuristic.ai.resource.ResourceWithCleanerInfo;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.OperationStatusRest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.AbstractResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Serge
 * Date: 6/14/2019
 * Time: 1:23 AM
 */
@RestController
@RequestMapping("/rest/v1/launchpad/batch")
@Slf4j
@Profile("launchpad")
@CrossOrigin
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'MANAGER', 'MASTER_OPERATOR')")
public class BatchRestController {

    private final BatchTopLevelService batchTopLevelService;
    private final LaunchpadContextService launchpadContextService;
    private final SourceCodeService sourceCodeService;

    @GetMapping("/batches")
    public BatchData.BatchesResult batches(
            @RequestParam(required = false, defaultValue = "false") boolean filterBatches,
            @PageableDefault(size = 20) Pageable pageable, Authentication authentication) {
        LaunchpadContext context = launchpadContextService.getContext(authentication);
        return batchTopLevelService.getBatches(pageable, context, false, filterBatches);
    }

    @GetMapping("/batch-exec-statuses")
    public BatchData.ExecStatuses batchExecStatuses(Authentication authentication) {
        LaunchpadContext context = launchpadContextService.getContext(authentication);
        return batchTopLevelService.getBatchExecStatuses(context);
    }

    @PostMapping("/batches-part")
    public BatchData.BatchesResult batchesPart(
            @RequestParam(required = false, defaultValue = "false") boolean filterBatches,
            @PageableDefault(size = 20) Pageable pageable, Authentication authentication) {
        LaunchpadContext context = launchpadContextService.getContext(authentication);
        return batchTopLevelService.getBatches(pageable, context, false, filterBatches);
    }

    @GetMapping(value = "/batch-add")
    public SourceCodeData.SourceCodesForCompany batchAdd(Authentication authentication) {
        LaunchpadContext context = launchpadContextService.getContext(authentication);
        return sourceCodeService.getAvailableSourceCodesForCompany(context);
    }

    @GetMapping("/batch-delete/{batchId}")
    public BatchData.Status processResourceDelete(@PathVariable Long batchId, Authentication authentication) {
        return batchTopLevelService.getProcessingResourceStatus(batchId, launchpadContextService.getContext(authentication), false);
    }

    @PostMapping("/batch-delete-commit")
    public OperationStatusRest processResourceDeleteCommit(Long batchId, Authentication authentication) {
        return batchTopLevelService.processResourceDeleteCommit(batchId, launchpadContextService.getContext(authentication), true);
    }

    @PostMapping(value = "/batch-upload-from-file")
    public OperationStatusRest uploadFile(final MultipartFile file, Long sourceCodeId, Authentication authentication) {
        BatchData.UploadingStatus uploadingStatus = batchTopLevelService.batchUploadFromFile(file, sourceCodeId, launchpadContextService.getContext(authentication));
        if (uploadingStatus.isErrorMessages()) {
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, uploadingStatus.getErrorMessages());
        }
        return OperationStatusRest.OPERATION_STATUS_OK;
    }

    @GetMapping(value= "/batch-status/{batchId}" )
    public BatchData.Status getProcessingResourceStatus(@PathVariable("batchId") Long batchId, Authentication authentication) {
        return batchTopLevelService.getProcessingResourceStatus(batchId, launchpadContextService.getContext(authentication), false);
    }

    @GetMapping(value= "/batch-download-result/{batchId}/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public HttpEntity<AbstractResource> downloadProcessingResult(
            HttpServletRequest request, @PathVariable("batchId") Long batchId,
            @SuppressWarnings("unused") @PathVariable("fileName") String fileName, Authentication authentication) throws IOException {
        LaunchpadContext context = launchpadContextService.getContext(authentication);
        final ResponseEntity<AbstractResource> entity;
        try {
            ResourceWithCleanerInfo resource = batchTopLevelService.getBatchProcessingResult(batchId, context, false);
            entity = resource.entity;
            request.setAttribute(Consts.RESOURCES_TO_CLEAN, resource.toClean);
        } catch (BinaryDataNotFoundException e) {
            return new ResponseEntity<>(Consts.ZERO_BYTE_ARRAY_RESOURCE, HttpStatus.GONE);
        }
        return entity;
    }

}
