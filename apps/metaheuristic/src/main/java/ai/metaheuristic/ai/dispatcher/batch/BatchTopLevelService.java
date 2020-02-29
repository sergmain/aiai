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

package ai.metaheuristic.ai.dispatcher.batch;

import ai.metaheuristic.ai.Consts;
import ai.metaheuristic.ai.Enums;
import ai.metaheuristic.ai.dispatcher.DispatcherContext;
import ai.metaheuristic.ai.dispatcher.batch.data.BatchStatusProcessor;
import ai.metaheuristic.ai.dispatcher.beans.Account;
import ai.metaheuristic.ai.dispatcher.beans.Batch;
import ai.metaheuristic.ai.dispatcher.beans.Ids;
import ai.metaheuristic.ai.dispatcher.beans.SourceCodeImpl;
import ai.metaheuristic.ai.dispatcher.data.BatchData;
import ai.metaheuristic.ai.dispatcher.data.SourceCodeData;
import ai.metaheuristic.ai.dispatcher.event.DispatcherEventService;
import ai.metaheuristic.ai.dispatcher.exec_context.ExecContextCreatorService;
import ai.metaheuristic.ai.dispatcher.exec_context.ExecContextService;
import ai.metaheuristic.ai.dispatcher.repositories.IdsRepository;
import ai.metaheuristic.ai.dispatcher.source_code.SourceCodeSelectorService;
import ai.metaheuristic.ai.dispatcher.source_code.SourceCodeValidationService;
import ai.metaheuristic.ai.dispatcher.variable.VariableService;
import ai.metaheuristic.ai.exceptions.BatchResourceProcessingException;
import ai.metaheuristic.ai.exceptions.BinaryDataNotFoundException;
import ai.metaheuristic.ai.resource.ResourceWithCleanerInfo;
import ai.metaheuristic.ai.utils.ControllerUtils;
import ai.metaheuristic.ai.utils.RestUtils;
import ai.metaheuristic.ai.yaml.batch.BatchParamsYaml;
import ai.metaheuristic.ai.yaml.batch.BatchParamsYamlUtils;
import ai.metaheuristic.api.EnumsApi;
import ai.metaheuristic.api.data.OperationStatusRest;
import ai.metaheuristic.api.data.source_code.SourceCodeApiData;
import ai.metaheuristic.api.data.task.TaskParamsYaml;
import ai.metaheuristic.commons.S;
import ai.metaheuristic.commons.utils.DirUtils;
import ai.metaheuristic.commons.utils.StrUtils;
import ai.metaheuristic.commons.utils.ZipUtils;
import ai.metaheuristic.commons.yaml.task.TaskParamsYamlUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ai.metaheuristic.ai.Consts.XML_EXT;
import static ai.metaheuristic.ai.Consts.ZIP_EXT;

/**
 * @author Serge
 * Date: 6/13/2019
 * Time: 11:52 PM
 */
@Slf4j
@Profile("dispatcher")
@Service
@RequiredArgsConstructor
public class BatchTopLevelService {

    private static final String ALLOWED_CHARS_IN_ZIP_REGEXP = "^[/\\\\A-Za-z0-9._-]*$";
    private static final Pattern zipCharsPattern = Pattern.compile(ALLOWED_CHARS_IN_ZIP_REGEXP);

    private final SourceCodeValidationService sourceCodeValidationService;
    private final VariableService variableService;
    private final BatchRepository batchRepository;
    private final BatchService batchService;
    private final BatchCache batchCache;
    private final DispatcherEventService dispatcherEventService;
    private final ExecContextService execContextService;
    private final ExecContextCreatorService execContextCreatorService;
    private final IdsRepository idsRepository;
    private final SourceCodeSelectorService sourceCodeSelectorService;

    public static final Function<String, Boolean> VALIDATE_ZIP_FUNCTION = BatchTopLevelService::isZipEntityNameOk;

    @SuppressWarnings("unused")
    public BatchData.ExecStatuses getBatchExecStatuses(DispatcherContext context) {
        //noinspection UnnecessaryLocalVariable
        BatchData.ExecStatuses execStatuses = new BatchData.ExecStatuses(batchRepository.getBatchExecStatuses(context.getCompanyId()));
        return execStatuses;
    }

    @Data
    @AllArgsConstructor
    public static class FileWithMapping {
        public File file;
        public String originName;
    }

    public static boolean isZipEntityNameOk(String name) {
        Matcher m = zipCharsPattern.matcher(name);
        return m.matches();
    }

    public BatchData.BatchesResult getBatches(Pageable pageable, DispatcherContext context, boolean includeDeleted, boolean filterBatches) {
        return getBatches(pageable, context.getCompanyId(), context.account, includeDeleted, context.account != null && filterBatches);
    }

    public BatchData.BatchesResult getBatches(Pageable pageable, Long companyUniqueId, Account account, boolean includeDeleted, boolean filterBatches) {
        if (filterBatches && account==null) {
            throw new IllegalStateException("(filterBatches && account==null)");
        }
        pageable = ControllerUtils.fixPageSize(20, pageable);
        Page<Long> batchIds;
        if (includeDeleted) {
            if (filterBatches) {
                batchIds = batchRepository.findAllForAccountByOrderByCreatedOnDesc(pageable, companyUniqueId, account.id);
            }
            else {
                batchIds = batchRepository.findAllByOrderByCreatedOnDesc(pageable, companyUniqueId);
            }
        }
        else {
            if (filterBatches) {
                batchIds = batchRepository.findAllForAccountExcludeDeletedByOrderByCreatedOnDesc(pageable, companyUniqueId, account.id);
            }
            else {
                batchIds = batchRepository.findAllExcludeDeletedByOrderByCreatedOnDesc(pageable, companyUniqueId);
            }
        }

        long total = batchIds.getTotalElements();

        List<BatchData.ProcessResourceItem> items = batchService.getBatches(batchIds);
        BatchData.BatchesResult result = new BatchData.BatchesResult();
        result.batches = new PageImpl<>(items, pageable, total);

        return result;
    }

    public BatchData.UploadingStatus batchUploadFromFile(final MultipartFile file, Long sourceCodeId, final DispatcherContext dispatcherContext) {
        String tempFilename = file.getOriginalFilename();
        if (S.b(tempFilename)) {
            return new BatchData.UploadingStatus("#995.040 name of uploaded file is null or blank");
        }
        // fix for the case when browser sends full path, ie Edge
        final String originFilename = new File(tempFilename.toLowerCase()).getName();

        String ext = StrUtils.getExtension(originFilename);
        if (ext==null) {
            return new BatchData.UploadingStatus(
                    "#995.043 file without extension, bad filename: " + originFilename);
        }
        if (!StringUtils.equalsAny(ext.toLowerCase(), ZIP_EXT, XML_EXT)) {
            return new BatchData.UploadingStatus("#995.046 only '.zip', '.xml' files are supported, bad filename: " + originFilename);
        }

        SourceCodeData.SourceCodesForCompany sourceCodesForCompany = sourceCodeSelectorService.getSourceCodeById(sourceCodeId, dispatcherContext.getCompanyId());
        if (sourceCodesForCompany.isErrorMessages()) {
            return new BatchData.UploadingStatus(sourceCodesForCompany.errorMessages);
        }
        SourceCodeImpl sourceCode = sourceCodesForCompany.items.isEmpty() ? null : (SourceCodeImpl) sourceCodesForCompany.items.get(0);
        if (sourceCode==null) {
            return new BatchData.UploadingStatus("#995.050 sourceCode wasn't found, sourceCodeId: " + sourceCodeId);
        }
        if (!sourceCode.getId().equals(sourceCodeId)) {
            return new BatchData.UploadingStatus("#995.038 Fatal error in configuration of sourceCode, report to developers immediately");
        }
        dispatcherEventService.publishBatchEvent(EnumsApi.DispatcherEventType.BATCH_FILE_UPLOADED, dispatcherContext.getCompanyId(), originFilename, file.getSize(), null, null, dispatcherContext );

        // TODO 2019-07-06 Do we need to validate the sourceCode here in case that there is another check?
        //  2019-10-28 it's working so left it as is until an issue with this will be found
        // validate the sourceCode
        SourceCodeApiData.SourceCodeValidation sourceCodeValidation = sourceCodeValidationService.validate(sourceCode);
        if (sourceCodeValidation.status != EnumsApi.SourceCodeValidateStatus.OK ) {
            return new BatchData.UploadingStatus("#995.060 validation of sourceCode was failed, status: " + sourceCodeValidation.status);
        }

        Batch b;
        try {
            // TODO 2020-02-24 lets save the file directly, without intermediate file
/*
            // tempDir will be deleted in processing thread
            File tempDir = DirUtils.createTempDir("batch-file-upload-");
            if (tempDir==null || tempDir.isFile()) {
                return new BatchData.UploadingStatus("#995.070 can't create temporary directory in " + System.getProperty("java.io.tmpdir"));
            }
            final File dataFile = File.createTempFile("uploaded-file-", ext, tempDir);
            log.debug("Start storing an uploaded file to disk");
            try(OutputStream os = new FileOutputStream(dataFile)) {
                IOUtils.copy(file.getInputStream(), os, 32000);
            }
*/

            ExecContextCreatorService.ExecContextCreationResult creationResult = execContextCreatorService.createExecContext(sourceCodeId, dispatcherContext);
            if (creationResult.isErrorMessages()) {
                throw new BatchResourceProcessingException("#995.075 Error creating execContext: " + creationResult.getErrorMessagesAsStr());
            }

            String startInputAs = creationResult.execContext.getExecContextParamsYaml().variables.startInputAs;
            variableService.save(
                    file.getInputStream(), file.getSize(), startInputAs,
                    originFilename, creationResult.execContext.getId(),
                    ""+idsRepository.save(new Ids()).id
            );

            b = new Batch(sourceCodeId, creationResult.execContext.getId(), Enums.BatchExecState.Stored,
                    dispatcherContext.getAccountId(), dispatcherContext.getCompanyId());

            BatchParamsYaml bpy = new BatchParamsYaml();
            bpy.username = dispatcherContext.account.username;
            b.params = BatchParamsYamlUtils.BASE_YAML_UTILS.toString(bpy);
            b = batchCache.save(b);

            dispatcherEventService.publishBatchEvent(
                    EnumsApi.DispatcherEventType.BATCH_CREATED, dispatcherContext.getCompanyId(),
                    sourceCode.uid, null, b.id, creationResult.execContext.getId(), dispatcherContext );

            final Batch batch = batchService.changeStateToPreparing(b.id);
            // TODO 2019-10-14 when batch is null tempDir won't be deleted, this is wrong behavior and need to be fixed
            if (batch==null) {
                return new BatchData.UploadingStatus("#995.080 can't find batch with id " + b.id);
            }

            log.info("The file {} was successfully stored to disk", originFilename);

            //noinspection unused
            int i=0;

            //noinspection UnnecessaryLocalVariable
            BatchData.UploadingStatus uploadingStatus = new BatchData.UploadingStatus(b.id, creationResult.execContext.getId());
            return uploadingStatus;
        }
        catch (Throwable th) {
            log.error("Error", th);
            return new BatchData.UploadingStatus("#995.120 can't load file, error: " + th.getMessage()+", class: " + th.getClass());
        }
    }

    public OperationStatusRest processResourceDeleteCommit(Long batchId, DispatcherContext context, boolean isVirtualDeletion) {
        return processResourceDeleteCommit(batchId, context.getCompanyId(), isVirtualDeletion);
    }

    public OperationStatusRest processResourceDeleteCommit(Long batchId, Long companyUniqueId, boolean isVirtualDeletion) {

        Batch batch = batchCache.findById(batchId);
        if (batch == null || !batch.companyId.equals(companyUniqueId)) {
            final String es = "#995.250 Batch wasn't found, batchId: " + batchId;
            log.info(es);
            return new OperationStatusRest(EnumsApi.OperationStatus.ERROR, es);
        }
        if (isVirtualDeletion) {
            if (!batch.deleted) {
                Batch b = batchRepository.findByIdForUpdate(batch.id, batch.companyId);
                b.deleted = true;
                batchCache.save(b);
            }
        }
        else {
            execContextService.deleteExecContext(batch.execContextId, companyUniqueId);
            batchCache.deleteById(batch.id);
        }
        return new OperationStatusRest(EnumsApi.OperationStatus.OK, "Batch #"+batch.id+" was deleted successfully.", null);
    }

    public BatchData.Status getProcessingResourceStatus(Long batchId, DispatcherContext context, boolean includeDeleted) {
        return getProcessingResourceStatus(batchId, context.getCompanyId(), includeDeleted);
    }

    public BatchData.Status getProcessingResourceStatus(Long batchId, Long companyUniqueId, boolean includeDeleted) {
        Batch batch = batchCache.findById(batchId);
        if (batch == null || !batch.companyId.equals(companyUniqueId) ||
                (!includeDeleted && batch.deleted)) {
            final String es = "#995.260 Batch wasn't found, batchId: " + batchId;
            log.warn(es);
            return new BatchData.Status(es);
        }
        BatchParamsYaml.BatchStatus status = batchService.updateStatus(batch);
        return new BatchData.Status(batchId, status.getStatus(), status.ok);
    }

    public ResourceWithCleanerInfo getBatchProcessingResult(Long batchId, DispatcherContext context, boolean includeDeleted) throws IOException {
        return getBatchProcessingResult(batchId, context.getCompanyId(), includeDeleted);
    }

    public ResourceWithCleanerInfo getBatchProcessingResult(Long batchId, Long companyUniqueId, boolean includeDeleted) throws IOException {
        Batch batch = batchCache.findById(batchId);
        if (batch == null || !batch.companyId.equals(companyUniqueId) ||
                (!includeDeleted && batch.deleted)) {
            final String es = "#995.260 Batch wasn't found, batchId: " + batchId;
            log.warn(es);
            return null;
        }
        ResourceWithCleanerInfo resource = new ResourceWithCleanerInfo();

        File resultDir = DirUtils.createTempDir("prepare-file-processing-result-");
        resource.toClean.add(resultDir);

        File zipDir = new File(resultDir, "zip");
        //noinspection ResultOfMethodCallIgnored
        zipDir.mkdir();

        BatchStatusProcessor status = batchService.prepareStatusAndData(batch, this::prepareZip, zipDir);

        File statusFile = new File(zipDir, "status.txt");
        FileUtils.write(statusFile, status.getStatus(), StandardCharsets.UTF_8);
        File zipFile = new File(resultDir, Consts.RESULT_ZIP);
        ZipUtils.createZip(zipDir, zipFile, status.renameTo);


        String filename = StrUtils.getName(status.originArchiveName) + Consts.ZIP_EXT;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // https://stackoverflow.com/questions/93551/how-to-encode-the-filename-parameter-of-content-disposition-header-in-http
        httpHeaders.setContentDisposition(ContentDisposition.parse(
                "filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())));
        resource.entity = new ResponseEntity<>(new FileSystemResource(zipFile), RestUtils.getHeader(httpHeaders, zipFile.length()), HttpStatus.OK);
        return resource;
    }

    public ResourceWithCleanerInfo getBatchOriginFile(Long batchId) throws IOException {
        Batch batch = batchCache.findById(batchId);
        if (batch == null) {
            final String es = "#995.260 Batch wasn't found, batchId: " + batchId;
            log.warn(es);
            return null;
        }
        ResourceWithCleanerInfo resource = new ResourceWithCleanerInfo();

        File resultDir = DirUtils.createTempDir("prepare-origin-file-");
        resource.toClean.add(resultDir);

        String originFilename = batchService.getUploadedFilename(batchId, batch.execContextId);
        File tempFile = File.createTempFile("batch-origin-file-", ".bin", resultDir);

        try {
            if (true) {
                throw new NotImplementedException("need to re-write with using execContextId and find the first vatiable in execContext");
            }
            String dataId = "1";
            variableService.storeToFile(dataId, tempFile);
        } catch (BinaryDataNotFoundException e) {
            String msg = "#990.375 Error store data to temp file, data doesn't exist in db, batchId " + batchId +
                    ", file: " + tempFile.getPath();
            log.error(msg);
            return null;
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentDisposition(ContentDisposition.parse("filename*=UTF-8''" + URLEncoder.encode(originFilename, StandardCharsets.UTF_8.toString())));
        resource.entity = new ResponseEntity<>(new FileSystemResource(tempFile), RestUtils.getHeader(httpHeaders, tempFile.length()), HttpStatus.OK);
        return resource;
    }

    // todo 2020-02-25 this method has to be re-written completely
    private boolean prepareZip(BatchService.PrepareZipData prepareZipData, File zipDir ) {
        if (true) {
            throw new NotImplementedException("Previous version was using list of exec contexts and in this method " +
                    "data was prepared only for one task (there was one task for one execContext)." +
                    "Not we have only one execContext with a number of tasks. So need to re-write to use taskId or something like that.");
        }
        final TaskParamsYaml taskParamYaml;
        try {
            taskParamYaml = TaskParamsYamlUtils.BASE_YAML_UTILS.to(prepareZipData.task.getParams());
        } catch (YAMLException e) {
            prepareZipData.bs.getErrorStatus().add(
                    "#990.350 " + prepareZipData.mainDocument + ", " +
                            "Task has broken data in params, status: " + EnumsApi.TaskExecState.from(prepareZipData.task.getExecState()) +
                            ", batchId:" + prepareZipData.batchId +
                            ", execContextId: " + prepareZipData.execContextId + ", " +
                            "taskId: " + prepareZipData.task.getId(), '\n');
            return false;
        }

        File tempFile;
        try {
            tempFile = File.createTempFile("doc-", ".xml", zipDir);
        } catch (IOException e) {
            String msg = "#990.370 Error create a temp file in "+zipDir.getAbsolutePath();
            log.error(msg);
            prepareZipData.bs.getGeneralStatus().add(msg,'\n');
            return false;
        }

        // all documents are sorted in zip folder
        prepareZipData.bs.renameTo.put("zip/" + tempFile.getName(), "zip/" + prepareZipData.mainDocument);


        // TODO 2020-01-30 need to re-write
/*
        try {
            variableService.storeToFile(taskParamYaml.taskYaml.outputResourceIds.values().iterator().next(), tempFile);
        } catch (BinaryDataNotFoundException e) {
            String msg = "#990.375 Error store data to temp file, data doesn't exist in db, code " +
                    taskParamYaml.taskYaml.outputResourceIds.values().iterator().next() +
                    ", file: " + tempFile.getPath();
            log.error(msg);
            prepareZipData.bs.getGeneralStatus().add(msg,'\n');
            return false;
        }
*/
        return true;
    }


}
