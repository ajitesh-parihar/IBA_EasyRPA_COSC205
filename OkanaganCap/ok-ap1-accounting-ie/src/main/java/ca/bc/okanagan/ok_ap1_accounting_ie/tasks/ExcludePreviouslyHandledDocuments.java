package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AccountingInfo;
import ca.bc.okanagan.ok_ap1_accounting_ie.repositories.AccountingInfoRepository;
import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.engine.annotation.ApTaskEntry;
import eu.ibagroup.easyrpa.engine.annotation.Input;
import eu.ibagroup.easyrpa.engine.annotation.InputToOutput;
import eu.ibagroup.easyrpa.engine.annotation.Output;
import eu.ibagroup.easyrpa.engine.apflow.ApTask;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@InputToOutput
@ApTaskEntry(
        name = "Exclude Previously Handled Documents",
        description = "Looks up accounting data in data store that has been already extracted."
)
public class ExcludePreviouslyHandledDocuments extends ApTask {

    @Input(ExportConstants.DOCUMENTS)
    @Output(ExportConstants.DOCUMENTS)
    private List<DocumentInfo> documents;

    @Output(ExportConstants.ACCOUNTING_INFO)
    private List<AccountingInfo> accountingInfoList;

    @Inject
    private AccountingInfoRepository accountingInfoRepository;

    @Override
    public void execute() throws Exception {
        accountingInfoList = new ArrayList<>();
        accountingInfoRepository.findAll().forEach(aInfo -> {
            DocumentInfo doc = documents.stream().filter(d ->
                    d.getCompanyNumber().equals(aInfo.getCompanyNumber())
                            && d.getName().equals(aInfo.getDocName())
                            && d.getPeriod().isEqual(aInfo.getPeriod())
            ).findFirst().orElse(null);
            if (doc != null) {
                documents.remove(doc);
                accountingInfoList.add(new AccountingInfo(aInfo));
            }
        });
    }

}
