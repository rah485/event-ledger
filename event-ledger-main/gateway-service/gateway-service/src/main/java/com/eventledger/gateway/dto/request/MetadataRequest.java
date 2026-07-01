package com.eventledger.gateway.dto.request;

import jakarta.validation.constraints.Size;

public record MetadataRequest(

        @Size(max = 100,
                message = "Source cannot exceed 100 characters")
        String source,

        @Size(max = 100,
                message = "Batch ID cannot exceed 100 characters")
        String batchId

) {
}