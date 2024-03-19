package ca.bc.okanagan.ok_ap1_accounting_ie.ap;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import eu.ibagroup.easyrpa.engine.boot.ApModuleRunner;
import eu.ibagroup.easyrpa.engine.boot.configuration.DevelopmentConfigurationModule;
import eu.ibagroup.easyrpa.engine.boot.configuration.OpenFrameworkModule;

import java.util.HashMap;
import java.util.Map;

public class AccountingInfoExtractionRunner {

    public static void main(String[] args) {

        Map<String, String> docsFilter = new HashMap<>();
        docsFilter.put("filling_type", "AA");
        docsFilter.put("type", "Full accounts");
        docsFilter.put("period", "2016");

        Map<String, Object> inputs = new HashMap<>();
//        inputs.put(ExportConstants.COMPANY, "MAP SOLAR PLC");
        inputs.put(ExportConstants.COMPANY, "GOODMAN HICHENS PLC");

//        inputs.put(ExportConstants.COMPANY, "ACCLOUD PLC");
        inputs.put(ExportConstants.REQUESTER, "demo.robot.tom@gmail.com");
        inputs.put(ExportConstants.DOCS_FILTER, docsFilter);

        ApModuleRunner.localLaunch(
                AccountingInfoExtractionModule.class, inputs,
                new DevelopmentConfigurationModule(args), new OpenFrameworkModule()
        );
    }
}