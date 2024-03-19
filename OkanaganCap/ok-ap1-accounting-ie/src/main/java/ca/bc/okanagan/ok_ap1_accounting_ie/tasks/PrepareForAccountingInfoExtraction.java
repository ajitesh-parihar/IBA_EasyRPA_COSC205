package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ApConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.model.AnnualFinancialStatement;
import eu.ibagroup.easyrpa.ap.dp.model.HocrInputJsonExt;
import eu.ibagroup.easyrpa.ap.dp.tasks.AutoTrainingTaskBase;
import eu.ibagroup.easyrpa.ap.dp.tasks.DocumentTaskBase;
import eu.ibagroup.easyrpa.ap.dp.tasks.DpDocumentTask;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.ContextId;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.DocumentId;
import eu.ibagroup.easyrpa.cs.controller.dto.MlModelVersionDto;
import eu.ibagroup.easyrpa.engine.annotation.*;
import eu.ibagroup.easyrpa.engine.exception.CoreError;
import eu.ibagroup.easyrpa.engine.exception.RpaException;
import eu.ibagroup.easyrpa.engine.task.ml.MlTask;
import eu.ibagroup.easyrpa.engine.task.ml.MlTaskData;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@InputToOutput(value = {ContextId.KEY, DocumentId.KEY})
@ApTaskEntry(
        name = "Prepare for Accounting Info Extraction",
        description = "Prepare inputs and configuration parameters for ML task that extracts accounting info."
)
public class PrepareForAccountingInfoExtraction extends DpDocumentTask<AnnualFinancialStatement>
        implements DocumentTaskBase<AnnualFinancialStatement>, AutoTrainingTaskBase<AnnualFinancialStatement> {

    @Configuration(value = "docs.extract.pages.by.keywords", defaultValue = "false")
    private Boolean isExtractPagesByKeywords;

    @Output(MlTask.ML_TASK_DATA_KEY)
    protected MlTaskData mlTaskData;

    @Input(value = AutoTrainingTaskBase.MODEL_VERSION_ID, required = false)
    protected Long modelVersionId;

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public void execute() {
        MlModelVersionDto runModel;
        if (modelVersionId != null) {
            runModel = getModelVersion(modelVersionId);
        } else {
            runModel = documentContext().getRunModel();
        }

        if (runModel != null) {
            log.info("Using run model {} ({})", runModel.getName(), runModel.getVersion());
            mlTaskData = prepareMlTask(() -> prepareMlTaskData(runModel));
        } else {
            throw new RpaException(CoreError.E3001, "There is no run model specified to run");
        }
    }

    protected MlTaskData prepareMlTaskData(MlModelVersionDto runModel) {
        HocrInputJsonExt hocrJson = HocrInputJsonExt.fromInputJson(documentContext());

        if (isExtractPagesByKeywords) {
            Set<Integer> foundPageIndexes = new HashSet<>();

            for (String key : getPageKeys()) {
                List<String> keywords = Arrays.stream(getConfigurationService().get(key).split(ApConstants.LIST_DELIMITER))
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

                int pageIndex = lookupPageByKeywords(hocrJson, keywords, foundPageIndexes);
                if (pageIndex >= 0) {
                    foundPageIndexes.add(pageIndex);
                }
            }

            if (foundPageIndexes.size() > 0) {
                hocrJson.leaveOnlyPages(foundPageIndexes);
            }
        }

        return hocrJson.prepareMlTaskData(documentContext(), runModel.getName(), runModel.getVersion());
    }

    private List<String> getPageKeys() {
        List<String> keywords = getConfigurationService().getConfiguration().keySet().stream()
                .filter(k -> k.matches(ApConstants.PAGE_KEYWORDS_CFG_PARAM_RE))
                .collect(Collectors.toList());
        Collections.reverse(keywords);
        return keywords;
    }

    private int lookupPageByKeywords(HocrInputJsonExt hocrJson, List<String> keywords, Set<Integer> foundPageIndexes) {
        int pageIndex = -1;
        int maxKeywordsPresent = 0;
        for (HocrInputJsonExt.HocrPage page : hocrJson.getPages()) {

            if (foundPageIndexes.contains(page.getPageNumber())) {
                // skip already found pages
                continue;
            }

            String text = page.getText().toLowerCase();

            int keywordsPresent = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword)) keywordsPresent++;
            }

            if (keywordsPresent > maxKeywordsPresent) {
                maxKeywordsPresent = keywordsPresent;
                pageIndex = page.getPageNumber();
            }
        }
        return pageIndex;
    }
}
