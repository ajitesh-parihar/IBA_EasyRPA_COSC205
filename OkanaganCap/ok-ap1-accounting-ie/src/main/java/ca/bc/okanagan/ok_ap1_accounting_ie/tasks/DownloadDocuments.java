package ca.bc.okanagan.ok_ap1_accounting_ie.tasks;

import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ApConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.constants.ExportConstants;
import ca.bc.okanagan.ok_ap1_accounting_ie.utils.NameUtils;
import ca.bc.okanagan.systems.uk_company_info_service.model.DocumentInfo;
import eu.ibagroup.easyrpa.engine.annotation.*;
import eu.ibagroup.easyrpa.engine.apflow.ApTask;
import eu.ibagroup.easyrpa.engine.service.StorageManager;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@Slf4j
@InputToOutput
@ApTaskEntry(
        name = "Download Documents",
        description = "Download given list of documents and puts them into local S3 storage."
)
public class DownloadDocuments extends ApTask {

    private static final Integer MAX_DOWNLOAD_ATTEMPTS = 3;

    @Configuration(value = "s3.bucket")
    private String s3Bucket;

    @Configuration(value = "docs.s3.temp.folder")
    private String s3DocsFolder;

    @Input(ExportConstants.DOCUMENTS)
    @Output(ExportConstants.DOCUMENTS)
    private List<DocumentInfo> documents;

    @Inject
    private StorageManager storageManager;

    @Override
    public void execute() {
        log.info("Download documents. Total amount of documents to download: {}", documents.size());
        for (DocumentInfo doc : documents) {
            log.info("Download document from: {}", doc.getUrl());
            downloadDocument(doc);
            log.info("Document downloaded successfully: {}", doc.getUrl());
        }
    }

    private void downloadDocument(DocumentInfo doc) {
        try {
            String docUuid = UUID.randomUUID().toString();
            String s3FilePath = String.format("%s/%s/%s/%s.pdf", s3DocsFolder, NameUtils.toSnakeCase(doc.getCompanyName()), doc.getName(), docUuid);

            int attempt = 1;
            while (true) {
                try (BufferedInputStream in = new BufferedInputStream(new URL(doc.getUrl()).openStream());
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                    byte[] buf = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buf, 0, 4096)) != -1) {
                        out.write(buf, 0, bytesRead);
                    }

                    storageManager.uploadFile(s3Bucket, s3FilePath, new ByteArrayInputStream(out.toByteArray()), ApConstants.PDF_MIME_TYPE, out.size());
                    doc.setUuid(docUuid);
                    doc.setS3Bucket(s3Bucket);
                    doc.setS3Path(s3FilePath);
                    break;
                } catch (Exception e) {
                    if (attempt >= MAX_DOWNLOAD_ATTEMPTS) {
                        throw e;
                    }
                    attempt++;
                    log.warn(String.format("%s attempt to download %s. Company number: %s", attempt, doc.getDescription(), doc.getCompanyNumber()));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(String.format("Downloading of %s has failed. Company number: %s", doc.getDescription(), doc.getCompanyNumber()), e);
        }
    }
}
