package ca.bc.okanagan.systems.uk_company_info_service.pages;

import ca.bc.okanagan.systems.uk_company_info_service.UkCompanyInfoService;
import eu.ibagroup.easyrpa.engine.rpa.page.WebPage;

public class GovUkBasePage extends WebPage {

    public void actionDelay() {
        ((UkCompanyInfoService) getApplication()).actionDelay();
    }

}
