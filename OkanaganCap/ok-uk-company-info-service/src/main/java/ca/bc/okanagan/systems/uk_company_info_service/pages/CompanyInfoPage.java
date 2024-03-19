package ca.bc.okanagan.systems.uk_company_info_service.pages;

import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.engine.annotation.AfterInit;
import eu.ibagroup.easyrpa.engine.rpa.element.BrowserElement;
import eu.ibagroup.easyrpa.engine.rpa.locator.BrowserSearch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompanyInfoPage extends GovUkBasePage {

    private BrowserSearch companyNameLc = BrowserSearch.xpath("//p[@class='heading-xlarge']");
    private BrowserSearch companyNumberLc = BrowserSearch.xpath("//*[@id='company-number']/strong");
    private BrowserSearch showFillingTypeCheckboxLc = BrowserSearch.xpath("//input[@id='show-filing-type']/..");
    private BrowserSearch accountsCheckBoxLc = BrowserSearch.xpath("//input[@id='filter-category-accounts']/..");
    private BrowserSearch documentRowsLc = BrowserSearch.xpath("//table[@id='fhTable']//tr");
    private BrowserSearch pageButtonsLc = BrowserSearch.xpath("//ul[@class='pager']//a[contains(@id, 'pageNo')]");
    private BrowserSearch nextPageBtnLc = BrowserSearch.id("nextButton");

    private static final String DOC_TYPE_LC_TPL = "//table[@id='fhTable']//tr[%s]/td[3]/strong";
    private static final String DOC_DESCRIPTION_LC_TPL = "//table[@id='fhTable']//tr[%s]/td[3]";
    private static final String DOC_FILLING_TYPE_LC_TPL = "//table[@id='fhTable']//tr[%s]/td[contains(@class, 'filing-type')]";
    private static final String DOC_DOWNLOAD_LINK_LC_TPL = "//table[@id='fhTable']//tr[%s]//a[contains(@class, 'download')]";

    private static final DateTimeFormatter PERIOD_PARSER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("MMddyyyy");
    private static final Pattern PERIOD_RE = Pattern.compile("^.+ (\\d{1,2} \\w+ \\d{4}).*$");

    @AfterInit
    public void init() {
        getDriver().waitFor(BrowserSearch.ExpectedConditions.visibilityOfElementLocated(documentRowsLc));
        actionDelay();
    }

    public CompanyInfoPage showFilingType() {
        while (true) {
            getDriver().waitForElement(showFillingTypeCheckboxLc).click();
            actionDelay();
            if (getDriver().waitFor(
                    BrowserSearch.ExpectedConditions.attributeContains(
                            showFillingTypeCheckboxLc,
                            "class",
                            "selected"
                    ),
                    true) != null) {
                break;
            }
            getDriver().refresh();
        }
        return this;
    }

    public CompanyInfoPage filterAccountsDocuments() {
        while (true) {
            BrowserElement accountsCheckBox = getDriver().waitForElement(accountsCheckBoxLc);
            final int initialRowsCount = getDriver().findElements(documentRowsLc).size();
            final int initialPageButtonsCount = getDriver().findElements(pageButtonsLc).size();
            accountsCheckBox.click();
            actionDelay();
            if (getDriver().waitFor(
                    driver -> initialRowsCount != driver.findElements(documentRowsLc).size() || initialPageButtonsCount != driver.findElements(pageButtonsLc).size(),
                    true
            ) != null) {
                break;
            }
            getDriver().refresh();
        }
        return this;
    }

    public List<DocumentInfo> getFilingHistoryDocuments() {
        List<DocumentInfo> docs = new ArrayList<>();
        String companyName = getDriver().waitForElement(companyNameLc).getText();
        String companyNumber = getDriver().waitForElement(companyNumberLc).getText();

        boolean hasNextPageBtn;
        do {
            hasNextPageBtn = !getDriver().findElements(nextPageBtnLc).isEmpty();
            int rowsCount = getDriver().findElements(documentRowsLc).size();
            for (int i = 2; i <= rowsCount; i++) {
                DocumentInfo docInfo = new DocumentInfo();
                BrowserSearch typeLc = BrowserSearch.xpath(String.format(DOC_TYPE_LC_TPL, i));
                BrowserSearch descriptionLc = BrowserSearch.xpath(String.format(DOC_DESCRIPTION_LC_TPL, i));
                BrowserSearch filingTypeLc = BrowserSearch.xpath(String.format(DOC_FILLING_TYPE_LC_TPL, i));
                BrowserSearch downloadPDFLinkLc = BrowserSearch.xpath(String.format(DOC_DOWNLOAD_LINK_LC_TPL, i));

                docInfo.setFillingType(getDriver().findElement(filingTypeLc).getText());
                docInfo.setDescription(getDriver().findElement(descriptionLc).getText());
                docInfo.setCompanyName(companyName);
                docInfo.setCompanyNumber(companyNumber);

                if (!getDriver().findElements(typeLc).isEmpty()) {
                    docInfo.setType(getDriver().findElement(typeLc).getText());
                }

                Matcher matcher = PERIOD_RE.matcher(docInfo.getDescription());
                if (matcher.find()) {
                    String dateStr = matcher.group(1);
                    docInfo.setPeriod(LocalDate.parse(dateStr, PERIOD_PARSER));
                }

                if (!getDriver().findElements(downloadPDFLinkLc).isEmpty()) {
                    docInfo.setUrl(getDriver().findElement(downloadPDFLinkLc).getAttribute("href"));
                }

                List<String> nameParts = new ArrayList<>();
                if (docInfo.getPeriod() != null) {
                    nameParts.add(PERIOD_FORMATTER.format(docInfo.getPeriod()));
                }
                nameParts.add(companyNumber);
                if (docInfo.getType() != null) {
                    nameParts.add(docInfo.getType().trim().replaceAll("[^\\w_]+", "_")
                            .replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase());
                } else {
                    nameParts.add("report");
                }
                docInfo.setName(String.join("_", nameParts));

                docs.add(docInfo);
            }
            if (hasNextPageBtn) {
                getDriver().findElement(nextPageBtnLc).click();
                actionDelay();
            }
        } while (hasNextPageBtn);

        return docs;
    }
}
