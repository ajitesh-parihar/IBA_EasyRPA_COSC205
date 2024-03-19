package eu.ibagroup.easyrpa.ap.dp;

import eu.ibagroup.easyrpa.ap.dp.annotation.Flow;
import eu.ibagroup.easyrpa.ap.dp.entity.DpDocument;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.ContextId;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.DocumentId;
import eu.ibagroup.easyrpa.ap.dp.tasks.to.DocumentSetInput;
import eu.ibagroup.easyrpa.engine.apflow.TaskInput;
import eu.ibagroup.easyrpa.engine.apflow.TaskOutput;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DocumentProcessorExt<D extends DpDocument> extends DocumentProcessor<D> {

    @Flow(name = PROCESS_DOCUMENTS)
    public CompletableFuture<TaskOutput> processDocuments(TaskInput rootInput) {
        Map<String, String> rootInputVariables = rootInput.getVariables();
        DocumentSetInput documentSetInput = getDocumentSetInput(rootInput);
        ContextId context = getParameter(rootInput, ContextId.class);
        // @formatter:off
        return emptyFlow(rootInput)
                .thenCompose(split(documentSetInput.uuidsToProcess(), (docInput, uuid) -> {
                    // Set single document input context
                    clearInput(docInput);
                    setParameter(docInput, new DocumentId(uuid));
                    setParameter(docInput, context);
                    log().info("Processing document {}:{}", uuid, context);
                    return processDocument(docInput).thenCompose(executeFlow(CLEANUP_INPUT));
                }).merge())
                .thenCompose(output -> {
                    rootInputVariables.forEach(output::set);
                    return CompletableFuture.completedFuture(output);
                });
        // @formatter:on
    }

    @Override
    public CompletableFuture<TaskOutput> processDocument(TaskInput docInput) {
        return processDocument(docInput, true);
    }
}
