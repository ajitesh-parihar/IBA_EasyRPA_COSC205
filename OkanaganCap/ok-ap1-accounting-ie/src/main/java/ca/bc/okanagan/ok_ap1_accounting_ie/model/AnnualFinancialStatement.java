package ca.bc.okanagan.ok_ap1_accounting_ie.model;

import ca.bc.okanagan.ok_ap1_accounting_ie.model.adapters.LocalDateAdapter;
import eu.ibagroup.easyrpa.ap.dp.entity.DpDocument;
import eu.ibagroup.easyrpa.persistence.annotation.Column;
import eu.ibagroup.easyrpa.persistence.annotation.Entity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity(value = "demo_ap_1_annual_financial_statements")
public class AnnualFinancialStatement extends DpDocument {

    @Getter
    @Setter
    @Column("company_number")
    private String companyNumber;

    @Getter
    @Setter
    @Column("company_name")
    private String companyName;

    @Getter
    @Setter
    @Column(value = "period", adapter = LocalDateAdapter.class)
    private LocalDate period;

    @Getter
    @Setter
    @Column("error_message")
    private String errorMessage;
}
