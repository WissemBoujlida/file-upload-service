package com.service.file.upload.s3.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record S3Event(
        @JsonProperty("EventName")
        String eventName,

        @JsonProperty("Records")
        List<S3EventRecord> records
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3EventRecord(
            String eventName,

            String eventTime,

            S3 s3
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record S3(
                S3Bucket bucket,

                S3Object object
        ) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record S3Bucket(
                    String name
            ) {
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record S3Object(
                    String key,

                    Long size,

                    String eTag,

                    String contentType
            ) {
            }
        }
    }
}
