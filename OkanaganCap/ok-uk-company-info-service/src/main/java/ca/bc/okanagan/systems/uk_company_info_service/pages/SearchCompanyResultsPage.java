package ca.bc.okanagan.systems.uk_company_info_service.pages;

import ca.bc.okanagan.systems.uk_company_info_service.model.CompanyInfo;
import eu.ibagroup.easyrpa.engine.annotation.AfterInit;
import eu.ibagroup.easyrpa.engine.rpa.locator.BrowserSearch;

import java.util.ArrayList;
import java.util.List;

public class SearchCompanyResultsPage extends GovUkBasePage {

    private BrowserSearch resultsListLc = BrowserSearch.id("results");
    private BrowserSearch resultLinksLc = BrowserSearch.xpath("//ul[@id='results']/li[@class='type-company']//a");

    @AfterInit
    public void init() {
        getDriver().waitFor(BrowserSearch.ExpectedConditions.visibilityOfElementLocated(resultsListLc));
        actionDelay();
    }

    public List<CompanyInfo> getResults() {
        List<CompanyInfo> results = new ArrayList<>();

        getDriver().findElements(resultLinksLc).forEach(a -> {
            CompanyInfo companyInfo = new CompanyInfo();
            companyInfo.setName(a.getText());
            companyInfo.setNumber(a.getAttribute("href").replaceAll("^.+/(\\w+)$", "$1"));
            results.add(companyInfo);
        });

        return results;
    }
}
