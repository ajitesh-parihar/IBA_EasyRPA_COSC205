package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AccountingInfo;
import eu.easyrpa.openframework.email.EmailMessage;
import eu.easyrpa.openframework.email.EmailSender;
import eu.easyrpa.openframework.excel.ExcelDocument;
import eu.easyrpa.openframework.excel.Row;
import eu.easyrpa.openframework.excel.Sheet;
import eu.ibagroup.easyrpa.engine.annotation.ApTaskEntry;
import eu.ibagroup.easyrpa.engine.annotation.Configuration;
import eu.ibagroup.easyrpa.engine.annotation.Input;
import eu.ibagroup.easyrpa.engine.apflow.ApTask;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ApTaskEntry(
        name = "Generate and Send Analytic Report",
        description = "Generate report with extracted accounting data for future analysis and send it to requester."
)
public class GenerateAndSendAnalyticReport extends ApTask {

    @Configuration(value = "analytic.report.tpl")
    private String analyticReportTplUrl;

    @Input(ExportConstants.REQUESTER)
    private String requester;

    @Input(ExportConstants.ACCOUNTING_INFO)
    private List<AccountingInfo> accountingInfoList;

    @Inject
    private EmailSender emailSender;

    private final DecimalFormat decimalFormat = new DecimalFormat("###.##");

    @Override
    public void execute() throws Exception {
        if (accountingInfoList.size() > 0) {

            accountingInfoList.sort(Comparator.comparing(AccountingInfo::getPeriod).reversed());

            try (ExcelDocument report = createAnalyticReport()) {
                Sheet sheet = report.selectSheet(0);

                AccountingInfo info = accountingInfoList.get(0);
                sheet.setValue("B2", info.getCompanyName());
                sheet.setValue("A3", info.getUnits());
                sheet.setValue("A16", info.getUnits());

                List<Integer> years = accountingInfoList.stream().map(ai -> ai.getPeriod().getYear()).collect(Collectors.toList());
                sheet.putRange("B3", years);
                sheet.putRange("B16", years);

                Row row = sheet.findRow("Profit after taxation");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getProfitAfterTax()==null ? "-" : decimalFormat.format(document.getProfitAfterTax())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "Profit");
                }

                row = sheet.findRow("Tax on profit");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getTaxOnProfit()==null ? "-" : decimalFormat.format(document.getTaxOnProfit())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "tax");
                }

                row = sheet.findRow("Profit before tax");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getProfitBeforeTax()==null ? "-" : decimalFormat.format(document.getProfitBeforeTax())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "profit before tax");
                }

                row = sheet.findRow("Operating Profit");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getOperatingProfit()==null ? "-" : decimalFormat.format(document.getOperatingProfit())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "operating profit");
                }

                row = sheet.findRow("Net Assets");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getNetAssets()==null ? "-" : decimalFormat.format(document.getNetAssets())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "net assets");
                }

                row = sheet.findRow("Net Interest");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getNetInterest()==null ? "-" : decimalFormat.format(document.getNetInterest())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "net assets");
                }

                row = sheet.findRow("(IT)");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getNetInterest()==null ? "-" : decimalFormat.format(document.getNetInterest())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "net assets");
                }

                row = sheet.findRow("EBIT");
                if (row != null) {
                    List<String> values = accountingInfoList.stream().map(document -> document.getOperatingProfit()==null ? "-" :  decimalFormat.format(document.getOperatingProfit())).collect(Collectors.toList());
                    row.putRange(1, values);
                } else {
                    log.warn("Field '{}' is not found in the report template.", "net assets");
                }

                new EmailMessage(emailSender)
                        .recipients(requester.split(";"))
                        .subject("Analytic Report")
                        .addHtml(String.format("<p>Hi</p><p>This is a generated report with extracted accounting data for %s.<p>", info.getCompanyName()))
                        .attach("analytic_report.xlsx", report.getInputStream(), report.getContentType())
                        .send();
            }
        }
    }

    private ExcelDocument createAnalyticReport() {
        try {
            if (analyticReportTplUrl.startsWith("http")) {
                try (BufferedInputStream in = new BufferedInputStream(new URL(analyticReportTplUrl).openStream());
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                    byte[] buf = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buf, 0, 4096)) != -1) {
                        out.write(buf, 0, bytesRead);
                    }
                    return new ExcelDocument(new ByteArrayInputStream(out.toByteArray()));
                }
            } else {
                return new ExcelDocument(analyticReportTplUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Creating of analytic report based on template '%s' has failed.", analyticReportTplUrl), e);
        }
    }
}
