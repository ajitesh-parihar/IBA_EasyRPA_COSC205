package ca.bc.okanagan.systems.uk_company_info_service.pages;

import eu.ibagroup.easyrpa.engine.rpa.element.BrowserElement;
import eu.ibagroup.easyrpa.engine.rpa.po.annotation.Wait;
import org.openqa.selenium.support.FindBy;

public class HomePage extends GovUkBasePage {

    @FindBy(id = "site-search-text")
    @Wait(waitFunc = Wait.WaitFunc.VISIBLE)
    private BrowserElement searchField;

    @FindBy(id = "search-submit")
    @Wait(waitFunc = Wait.WaitFunc.VISIBLE)
    private BrowserElement searchBtn;


    public SearchCompanyResultsPage searchCompany(String companyName) {
        searchField.click();
        actionDelay();
        searchField.sendKeys(companyName);
        actionDelay();
        searchBtn.click();
        return createPage(SearchCompanyResultsPage.class);
    }
}
