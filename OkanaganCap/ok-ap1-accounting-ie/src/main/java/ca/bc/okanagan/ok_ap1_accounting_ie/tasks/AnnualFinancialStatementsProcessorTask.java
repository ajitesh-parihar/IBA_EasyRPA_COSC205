package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.model.AnnualFinancialStatement;
import ca.bc.okanagan.ok_ap1_accounting_ie.repositories.AnnualFinancialStatementRepository;
import ca.bc.okanagan.ok_ap1_accounting_ie.validators.AccountingInfoExtractor;
import eu.ibagroup.easyrpa.ap.dp.tasks.DpDocumentsTask;
import lombok.Getter;

import javax.inject.Inject;

public abstract class AnnualFinancialStatementsProcessorTask extends DpDocumentsTask<AnnualFinancialStatement>
        implements AccountingInfoExtractor {

    @Inject
    @Getter
    private AnnualFinancialStatementRepository documentRepository;
}
