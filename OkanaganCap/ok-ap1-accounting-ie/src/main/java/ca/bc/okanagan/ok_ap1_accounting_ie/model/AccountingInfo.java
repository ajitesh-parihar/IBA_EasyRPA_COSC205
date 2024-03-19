package ca.bc.okanagan.ok_ap1_accounting_ie.model;

import ca.bc.okanagan.ok_ap1_accounting_ie.model.adapters.LocalDateAdapter;
import eu.ibagroup.easyrpa.persistence.annotation.Column;
import eu.ibagroup.easyrpa.persistence.annotation.Entity;
import eu.ibagroup.easyrpa.persistence.annotation.EntityType;
import eu.ibagroup.easyrpa.persistence.annotation.Id;
import lombok.Data;

import java.time.LocalDate;

@Entity(value = "demo_ap1_accounting_info", type = EntityType.DATASTORE)
@Data
public class AccountingInfo {

    @Id
    @Column("uuid")
    private String uuid;

    @Column("company_number")
    private String companyNumber;

    @Column("company_name")
    private String companyName;

    @Column("doc_name")
    private String docName;

    @Column("doc_uuid")
    private String docUuid;

    @Column(value = "period", adapter = LocalDateAdapter.class)
    private LocalDate period;

    @Column("profit_after_tax")
    private Double profitAfterTax;

    @Column("units")
    private String units;

    @Column("operating_profit")
    private Double operatingProfit;

    @Column("profit_before_tax")
    private Double profitBeforeTax;

    @Column("tax_on_profit")
    private Double taxOnProfit;

    @Column("net_assets")
    private Double netAssets;

    @Column("net_interest")
    private Double netInterest;

    public AccountingInfo() {
    }

    public AccountingInfo(AccountingInfo aInfo) {
        uuid = aInfo.uuid;
        companyNumber = aInfo.companyNumber;
        companyName = aInfo.companyName;
        docName = aInfo.docName;
        period = aInfo.period;
        profitAfterTax = aInfo.profitAfterTax;
        operatingProfit = aInfo.operatingProfit;
        profitBeforeTax = aInfo.profitBeforeTax;
        taxOnProfit = aInfo.taxOnProfit;
        netAssets = aInfo.netAssets;
        netInterest = aInfo.netInterest;
        units = aInfo.units;
    }
}
