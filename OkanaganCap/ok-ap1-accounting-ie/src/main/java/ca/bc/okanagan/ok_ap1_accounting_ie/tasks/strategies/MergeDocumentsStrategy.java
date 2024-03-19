package ca.bc.okanagan.ok_ap1_accounting_ie.tasks.strategies;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.engine.annotation.ApTaskEntry;
import eu.ibagroup.easyrpa.engine.apflow.ParallelGateway;
import eu.ibagroup.easyrpa.engine.apflow.TaskOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApTaskEntry(
        name = "Merge Documents",
        description = "Merges documents into one list"
)
public class MergeDocumentsStrategy extends ParallelGateway {

    @SuppressWarnings("unchecked")
    protected TaskOutput merge(TaskOutput to, List<TaskOutput> outputs) {
        List<DocumentInfo> mergedList = new ArrayList<>();
        for (TaskOutput taskOutput : outputs) {
            DocumentInfo doc = taskOutput.get(ExportConstants.DOC, DocumentInfo.class);
            if (doc != null) {
                mergedList.add(doc);
            } else {
                List<DocumentInfo> docs = taskOutput.get(ExportConstants.DOCUMENTS, List.class);
                if (docs != null) {
                    mergedList.addAll(docs);
                }
            }
        }
        to.set(ExportConstants.DOCUMENTS, mergedList);
        copyOtherVariables(to, outputs.get(0), ExportConstants.DOCUMENTS);
        return to;
    }

    private void copyOtherVariables(TaskOutput to, TaskOutput from, String... excludes) {
        List<String> ignoreKeys = Arrays.asList(excludes);
        from.getVariables().forEach((key, value) -> {
            if (!ignoreKeys.contains(key)) {
                to.set(key, value);
            }
        });
    }
}
