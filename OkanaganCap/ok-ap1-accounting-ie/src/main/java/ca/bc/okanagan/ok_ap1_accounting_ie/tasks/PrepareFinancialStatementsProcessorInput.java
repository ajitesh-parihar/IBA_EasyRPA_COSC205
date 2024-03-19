package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ApConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AnnualFinancialStatement;
import ca.bc.okanagan.ok_ap1_accounting_ie.repositories.AnnualFinancialStatementRepository;
import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.ap.dp.context.DataStoreContext;
import eu.ibagroup.easyrpa.ap.dp.context.DocumentContext;
import eu.ibagroup.easyrpa.ap.dp.context.DocumentContextFactory;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.ContextId;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.DocumentSetInput;
import eu.ibagroup.easyrpa.engine.annotation.ApTaskEntry;
import eu.ibagroup.easyrpa.engine.annotation.Input;
import eu.ibagroup.easyrpa.engine.annotation.InputToOutput;
import eu.ibagroup.easyrpa.engine.annotation.Output;
import eu.ibagroup.easyrpa.engine.apflow.ApTask;
import eu.ibagroup.easyrpa.engine.service.InstanceService;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;

@Slf4j
@InputToOutput
@ApTaskEntry(
        name = "Prepare Financial Statements Processor Input",
        description = "Import annual financial statements into context documents set to process them by document processor."
)
public class PrepareFinancialStatementsProcessorInput extends ApTask implements DocumentContextFactory<AnnualFinancialStatement> {

    @Input(ExportConstants.DOCUMENTS)
    private List<DocumentInfo> documents;

    @Inject
    @Getter
    private InstanceService instanceService;

    @Inject
    @Getter
    private AnnualFinancialStatementRepository documentRepository;

    @Output(DocumentSetInput.KEY)
    private DocumentSetInput documentSetInput;

    @Output(ContextId.KEY)
    private ContextId contextId;

    @Override
    @SneakyThrows
    public void execute() {
        contextId = new ContextId("demo_ap1_accounting_info_extraction", DataStoreContext.HANDLER_NAME);
        documentSetInput = new DocumentSetInput();

        DocumentContext<AnnualFinancialStatement> context = contextHandler(contextId);

        for (DocumentInfo docInfo : documents) {
            AnnualFinancialStatement doc = context.newDocument();
            doc.setName(docInfo.getName());
            doc.setCompanyNumber(docInfo.getCompanyNumber());
            doc.setCompanyName(docInfo.getCompanyName());
            doc.setPeriod(docInfo.getPeriod());
            doc.setAutoTrainingJson(null);
            doc.setS3Path(String.format("%s/%s", context.getS3BasePath(), FilenameUtils.getName(docInfo.getS3Path())));

            InputStream docFileIs = context.getStorageManager().getFile(docInfo.getS3Bucket(), docInfo.getS3Path());
            context.getStorageManager().uploadFile(context.getBucket(), doc.getS3Path(), docFileIs, ApConstants.PDF_MIME_TYPE, docFileIs.available());
            doc.setUrl(context.getStorageManager().getFileLink(context.getBucket(), doc.getS3Path()));

            context.save(doc);

            documentSetInput.getUuids().add(doc.getUuid());
        }
    }
}
