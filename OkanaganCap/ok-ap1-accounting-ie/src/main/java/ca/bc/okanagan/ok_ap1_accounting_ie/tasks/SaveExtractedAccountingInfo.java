package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AccountingInfo;
import ca.bc.okanagan.ok_ap1_accounting_ie.repositories.AccountingInfoRepository;
import eu.ibagroup.easyrpa.engine.annotation.ApTaskEntry;
import eu.ibagroup.easyrpa.engine.annotation.Input;
import eu.ibagroup.easyrpa.engine.annotation.InputToOutput;
import eu.ibagroup.easyrpa.engine.annotation.Output;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@InputToOutput
@ApTaskEntry(
        name = "Save Extracted Accounting Info",
        description = "Save extracted accounting info into data store."
)
public class SaveExtractedAccountingInfo extends AnnualFinancialStatementsProcessorTask {

    @Input(value = ExportConstants.ACCOUNTING_INFO)
    @Output(ExportConstants.ACCOUNTING_INFO)
    private List<AccountingInfo> accountingInfoList;

    @Inject
    private AccountingInfoRepository accountingInfoRepository;

    @Override
    public void execute() throws Exception {
        for (String uuid : getDocumentSetInput().uuidsToProcess()) {
            documentContext().link(uuid);
            AccountingInfo accountingInfo = getAccountingInfo();
            accountingInfoRepository.save(accountingInfo);
            accountingInfoList.add(accountingInfo);
        }
    }

}
