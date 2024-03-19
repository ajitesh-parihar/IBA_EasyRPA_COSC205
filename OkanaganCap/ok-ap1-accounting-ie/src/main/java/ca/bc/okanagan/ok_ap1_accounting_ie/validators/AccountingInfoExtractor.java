package ca.bc.okanagan.ok_ap1_accounting_ie.validators;

import ca.bc.okanagan.ok_ap1_accounting_ie.model.AccountingInfo;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AnnualFinancialStatement;
import eu.ibagroup.easyrpa.ap.iedp.parsers.CurrencyParser;
import eu.ibagroup.easyrpa.ap.iedp.validation.IeValidatorBase;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.bc.okanagan.ok_ap1_accounting_ie.constants.AccountingFields.*;

public interface AccountingInfoExtractor extends IeValidatorBase<AnnualFinancialStatement> {

    default AccountingInfo getAccountingInfo() {
        AnnualFinancialStatement doc = documentContext().getDocument();
        AccountingInfo accInfo = new AccountingInfo();

        accInfo.setCompanyNumber(doc.getCompanyNumber());
        accInfo.setCompanyName(doc.getCompanyName());
        accInfo.setDocName(doc.getName());
        accInfo.setDocUuid(doc.getUuid());
        accInfo.setPeriod(doc.getPeriod());

        int multiplier = getValueAs(UNITS, content -> {
            if (content.toLowerCase().contains("m")) return 1000000;
            else {
                final Pattern pattern = Pattern.compile("((0)+)", Pattern.MULTILINE);
                final Matcher matcher = pattern.matcher(content);
                if (matcher.find()) return (int) Math.pow(10, matcher.group().length());
            }
            return 1;
        });

        Currency currency = getValueAs(UNITS, new CurrencyParser());
        accInfo.setUnits(currency != null ? currency.getCurrencyCode() : null);

        BigDecimal profitBeforeTax = getValueAsAmount(PROFIT_BEFORE_TAX);
        accInfo.setProfitBeforeTax(profitBeforeTax != null ? profitBeforeTax.doubleValue() * multiplier : null);

        BigDecimal profitAfterTax = getValueAsAmount(PROFIT_AFTER_TAX);
        accInfo.setProfitAfterTax(profitAfterTax != null ? profitAfterTax.doubleValue() * multiplier : null);

        BigDecimal tax = getValueAsAmount(TAX_ON_PROFIT);
        accInfo.setTaxOnProfit(tax != null ? tax.doubleValue() * multiplier : null);

        BigDecimal operatingProfit = getValueAsAmount(OPERATING_PROFIT);
        accInfo.setOperatingProfit(operatingProfit != null ? operatingProfit.doubleValue() * multiplier : null);

        BigDecimal netAssets = getValueAsAmount(NET_ASSETS);
        accInfo.setNetAssets(netAssets != null ? netAssets.doubleValue() * multiplier : null);

        return accInfo;
    }

}
