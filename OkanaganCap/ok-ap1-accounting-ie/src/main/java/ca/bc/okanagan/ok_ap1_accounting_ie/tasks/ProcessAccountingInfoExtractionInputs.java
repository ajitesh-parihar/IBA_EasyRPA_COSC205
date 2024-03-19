package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import eu.ibagroup.easyrpa.engine.annotation.ApTaskEntry;
import eu.ibagroup.easyrpa.engine.annotation.Input;
import eu.ibagroup.easyrpa.engine.annotation.InputToOutput;
import eu.ibagroup.easyrpa.engine.annotation.Output;
import eu.ibagroup.easyrpa.engine.apflow.ApTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@InputToOutput(ExportConstants.DOCS_FILTER)
@ApTaskEntry(
        name = "Process Accounting Info Extraction Inputs",
        description = "Check provided input parameters for process run and prepare inputs for further steps."
)
public class ProcessAccountingInfoExtractionInputs extends ApTask {

    @Input(value = ExportConstants.COMPANY, required = false)
    private String companyName;

    @Input(value = ExportConstants.REQUESTER, required = false)
    @Output
    private String requester;

    @Output(ExportConstants.IS_INPUTS_VALID)
    private Boolean isInputsValid;

    @Output(ExportConstants.COMPANIES)
    private List<String> companiesList;

    @Override
    public void execute() {
        isInputsValid = StringUtils.isNotBlank(requester) && StringUtils.isNotBlank(companyName);
        if (isInputsValid) {
            companiesList = new ArrayList<>();
            companiesList.add(companyName);
        }
    }
}
