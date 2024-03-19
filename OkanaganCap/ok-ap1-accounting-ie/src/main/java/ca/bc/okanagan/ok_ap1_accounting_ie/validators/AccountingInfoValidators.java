package ca.bc.okanagan.ok_ap1_accounting_ie.validators;

import ca.bc.okanagan.ok_ap1_accounting_ie.model.AccountingInfo;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AnnualFinancialStatement;
import ca.bc.okanagan.ok_ap1_accounting_ie.repositories.AnnualFinancialStatementRepository;
import eu.ibagroup.easyrpa.ap.dp.annotation.PostProcessorMethod;
import eu.ibagroup.easyrpa.ap.dp.annotation.PostProcessorStrategies;
import eu.ibagroup.easyrpa.ap.dp.postprocessing.BasePostProcessor;
import eu.ibagroup.easyrpa.ap.dp.validation.ValidationMessage;
import lombok.Getter;

import javax.inject.Inject;

@PostProcessorStrategies("ie")
public class AccountingInfoValidators extends BasePostProcessor<AnnualFinancialStatement> implements AccountingInfoExtractor {

    @Inject
    @Getter
    private AnnualFinancialStatementRepository documentRepository;

    @PostProcessorMethod("checkFinancialStatementAmounts")
    public void checkFinancialStatementAmounts() {

        if (isValidDocument()) {
            AccountingInfo accInfo = getAccountingInfo();

            Double operatingProfit = accInfo.getOperatingProfit();
            Double profitBeforeTax = accInfo.getProfitBeforeTax();
            Double profitAfterTax = accInfo.getProfitAfterTax();
            Double tax = accInfo.getTaxOnProfit();

            if (operatingProfit == null) operatingProfit = 0d;
            if (profitBeforeTax == null) profitBeforeTax = 0d;
            if (profitAfterTax == null) profitAfterTax = 0d;
            if (tax == null) tax = 0d;

            if (Math.abs(operatingProfit) > Math.abs(profitAfterTax * 10)) {
                addMessages(ValidationMessage.error("Operating Profit has invalid value"));
            }

            if (Math.abs(profitBeforeTax - profitAfterTax) != Math.abs(tax)) {
                addMessages(ValidationMessage.error("Profit after taxes plus taxes are not matches with profit before taxes"));
            }
        }
    }

}
