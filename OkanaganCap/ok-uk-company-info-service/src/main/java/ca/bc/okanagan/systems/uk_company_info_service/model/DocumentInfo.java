package ca.bc.okanagan.systems.uk_company_info_service.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentInfo {

    private String uuid;
    private String name;
    private String type;
    private String description;
    private String fillingType;
    private String companyName;
    private String companyNumber;
    private LocalDate period;
    private String url;
    private String s3Bucket;
    private String s3Path;
}
