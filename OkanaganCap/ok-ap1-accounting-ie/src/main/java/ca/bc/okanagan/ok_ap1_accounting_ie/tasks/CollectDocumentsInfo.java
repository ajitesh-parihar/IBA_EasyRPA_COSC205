package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ApConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.systems.uk_company_info_service.UkCompanyInfoService;
import ca.bc.okanagan.systems.uk_company_info_service.model.CompanyInfo;
import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.engine.annotation.*;
import eu.ibagroup.easyrpa.engine.apflow.ApTask;
import eu.ibagroup.easyrpa.engine.rpa.driver.BrowserDriver;
import eu.ibagroup.easyrpa.engine.rpa.driver.DriverParams;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@InputToOutput(ExportConstants.REQUESTER)
@ApTaskEntry(
        name = "Collect Available Documents Info",
        description = "Searches specified companies in gov.uk and collects available financial reports."
)
public class CollectDocumentsInfo extends ApTask {

    @Driver(param = {@DriverParameter(key = DriverParams.Browser.SELENIUM_NODE_CAPABILITIES, initializerName = DriverParams.BrowserCapabilities.CHROME),
            @DriverParameter(key = DriverParams.Browser.PAGE_LOAD_TIMEOUT_SECONDS, direct = "30"),
            @DriverParameter(key = DriverParams.Browser.IMPLICITLY_WAIT_TIMEOUT_SECONDS, direct = "0"),
            @DriverParameter(key = DriverParams.All.EXPLICITLY_WAIT_TIMEOUT_SECONDS, direct = "5"),
            @DriverParameter(key = DriverParams.All.EXPLICITLY_WAIT_POLLING_INTERVAL_MILLISECONDS, direct = "1000")})
    private BrowserDriver browserDriver;

    @Configuration(value = "robot.actions.delay.sec")
    private Integer actionsDelay;

    @Input(ExportConstants.COMPANIES)
    private List<String> companiesList;

    @Input(value = ExportConstants.DOCS_FILTER, required = false)
    private Map<String, String> docsFilter;

    @Output(ExportConstants.DOCUMENTS)
    private List<DocumentInfo> documents = new ArrayList<>();

    @Override
    public void execute() {
        log.info("Collect documents for companies: {}", companiesList);

        UkCompanyInfoService ukCompanyInfoService = new UkCompanyInfoService(browserDriver, actionsDelay);

        for (String company : companiesList) {
            List<CompanyInfo> results = ukCompanyInfoService.searchCompany(company).getResults();
            if (results.isEmpty()) {
                log.warn("Company '{}' is not found", company);
                continue;
            }
            CompanyInfo companyInfo = results.get(0);
            if (!company.equalsIgnoreCase(companyInfo.getName())) {
                log.warn("Company '{}' is not found. Found companies: {}",
                        company,
                        results.stream().map(CompanyInfo::getName).collect(Collectors.joining(", "))
                );
                continue;
            }

            List<DocumentInfo> availableDocuments = ukCompanyInfoService.openCompanyFilingHistory(companyInfo.getNumber())
                    .showFilingType()
                    .filterAccountsDocuments()
                    .getFilingHistoryDocuments()
                    .stream().filter(d -> d.getUrl() != null)
                    .collect(Collectors.toList());

            log.info("Found documents of company '{}'", company);
            availableDocuments.forEach(d -> log.info("{}", d));

            if (docsFilter == null) {
                docsFilter = new HashMap<>();
                docsFilter.put("filling_type", "AA");
                docsFilter.put("type", "Full accounts");
            }

            documents.addAll(availableDocuments.stream()
                    .filter(d -> {
                        if (d.getUrl() == null) {
                            return false;
                        }
                        String type = docsFilter.get("type");
                        if (type != null && !Arrays.asList(type.split(ApConstants.LIST_DELIMITER)).contains(d.getType())) {
                            return false;
                        }
                        String fillingType = docsFilter.get("filling_type");
                        if (fillingType != null && !Arrays.asList(fillingType.split(ApConstants.LIST_DELIMITER)).contains(d.getFillingType())) {
                            return false;
                        }
                        String period = docsFilter.get("period");
                        return period == null || Arrays.asList(period.split(ApConstants.LIST_DELIMITER)).contains("" + d.getPeriod().getYear());
                    })
                    .collect(Collectors.toList()));
        }
    }
}
