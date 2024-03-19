package ca.bc.okanagan.ok_ap1_accounting_ie.ap;

import ca.bc.okanagan.ok_ap1_accounting_ie.model.AnnualFinancialStatement;
import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.CollectDocumentsInfo;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.DownloadDocuments;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.ExcludePreviouslyHandledDocuments;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.GenerateAndSendAnalyticReport;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.PrepareFinancialStatementsProcessorInput;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.PrepareForAccountingInfoExtraction;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.ProcessAccountingInfoExtractionInputs;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.SaveExtractedAccountingInfo;
import ca.bc.okanagan.ok_ap1_accounting_ie.tasks.strategies.MergeDocumentsStrategy;
import ca.bc.okanagan.ok_ap1_accounting_ie.utils.BatchUtils;
import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.ap.dp.DocumentProcessorExt;
import eu.ibagroup.easyrpa.ap.dp.annotation.Flow;
import eu.ibagroup.easyrpa.ap.iedp.IeDocumentProcessorBase;
import eu.ibagroup.easyrpa.engine.annotation.ApModuleEntry;
import eu.ibagroup.easyrpa.engine.annotation.Configuration;
import eu.ibagroup.easyrpa.engine.apflow.TaskInput;
import eu.ibagroup.easyrpa.engine.apflow.TaskOutput;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@ApModuleEntry(
        name = "[OKAP-1] Accounting Info Extraction",
        description = "This process demonstrates extraction of accounting information from financial reports of UK companies."
)
public class AccountingInfoExtractionModule extends DocumentProcessorExt<AnnualFinancialStatement> implements IeDocumentProcessorBase {

    @Configuration(value = "docs.download.threads.amount", defaultValue = "1")
    private Integer downloadThreadsAmount;

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public CompletableFuture<TaskOutput> processRun(TaskInput root) {

        TaskOutput output = execute(getInput(), ProcessAccountingInfoExtractionInputs.class).get();
        if (!output.get(ExportConstants.IS_INPUTS_VALID, Boolean.class)) {
            return emptyFlow(output);
        }

        output = execute(output, CollectDocumentsInfo.class)
                .thenCompose(execute(ExcludePreviouslyHandledDocuments.class))
                .get();

        List<DocumentInfo> docsToProcess = output.get(ExportConstants.DOCUMENTS, List.class);

        if (docsToProcess.size() > 0) {
            List<List<DocumentInfo>> docBatches =  BatchUtils.splitIntoBatches(docsToProcess, downloadThreadsAmount);

            output = split(output, docBatches, this::downloadDocuments).merge(MergeDocumentsStrategy.class)
                    .thenCompose(execute(PrepareFinancialStatementsProcessorInput.class))
                    .thenCompose(processDocuments())
                    .thenCompose(execute(SaveExtractedAccountingInfo.class))
                    .get();
        }

        return execute(output, GenerateAndSendAnalyticReport.class);
    }

    @Flow(name = PREPARE_ML, docType = "[DEMOAP-1] Annual Financial Statement")
    public CompletableFuture<TaskOutput> prepareForAccountingInfoExtraction(TaskInput input) {
        return execute(input, PrepareForAccountingInfoExtraction.class);
    }

    @Flow(name = HT_STORE_TO_DOC_SET, docType = "[DEMOAP-1] Annual Financial Statement")
    public CompletableFuture<TaskOutput> documentExportFlowIdp(TaskInput input) {
        return executeFlow(input, DOCUMENT_EXPORT_TO_DOC_SET);
    }

    private CompletableFuture<TaskOutput> downloadDocuments(TaskInput input, List<DocumentInfo> documents) {
        input.set(ExportConstants.DOCUMENTS, documents);
        return execute(input, DownloadDocuments.class);
    }
}
