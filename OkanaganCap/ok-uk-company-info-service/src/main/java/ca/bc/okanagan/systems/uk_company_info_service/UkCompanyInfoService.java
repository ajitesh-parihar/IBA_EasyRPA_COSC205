package ca.bc.okanagan.systems.uk_company_info_service;


import ca.bc.okanagan.systems.uk_company_info_service.pages.CompanyInfoPage;
import ca.bc.okanagan.systems.uk_company_info_service.pages.HomePage;
import ca.bc.okanagan.systems.uk_company_info_service.pages.SearchCompanyResultsPage;
import eu.ibagroup.easyrpa.engine.rpa.Application;
import eu.ibagroup.easyrpa.engine.rpa.driver.BrowserDriver;
import eu.ibagroup.easyrpa.engine.rpa.element.BrowserElement;
import lombok.SneakyThrows;

import java.net.URLEncoder;

public class UkCompanyInfoService extends Application<BrowserDriver, BrowserElement> {

    public static final String SERVICE_BASE_URL = "https://find-and-update.company-information.service.gov.uk";

    private static final String SEARCH_COMPANY_URL = SERVICE_BASE_URL + "/search/companies?q=%s";
    private static final String COMPANY_FILING_HISTORY_URL = SERVICE_BASE_URL + "/company/%s/filing-history";

    private Integer actionsDelay;


    public UkCompanyInfoService(BrowserDriver driver) {
        super(driver);
        this.actionsDelay = actionsDelay;
    }

    public UkCompanyInfoService(BrowserDriver driver, Integer actionsDelay) {
        super(driver);
        this.actionsDelay = actionsDelay;
    }

    @Override
    public HomePage open(String... args) {
        getDriver().get(SERVICE_BASE_URL);
        getDriver().maximize();
        return createPage(HomePage.class);
    }

    public SearchCompanyResultsPage searchCompany(String companyName) {
        try {
            String url = String.format(SEARCH_COMPANY_URL, URLEncoder.encode(companyName, "UTF-8"));
            getDriver().get(url);
            getDriver().maximize();
            return createPage(SearchCompanyResultsPage.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Searching of company '%s' has failed.", companyName), e);
        }
    }

    public CompanyInfoPage openCompanyFilingHistory(String companyNumber) {
        try {
            String url = String.format(COMPANY_FILING_HISTORY_URL, companyNumber);
            getDriver().get(url);
            getDriver().maximize();
            return createPage(CompanyInfoPage.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Opening of filing history of company with number '%s' has failed.", companyNumber),
                    e
            );
        }
    }

    @SneakyThrows
    public void actionDelay() {
        if (actionsDelay != null) {
            Thread.sleep(1000L * actionsDelay);
        }
    }
}
