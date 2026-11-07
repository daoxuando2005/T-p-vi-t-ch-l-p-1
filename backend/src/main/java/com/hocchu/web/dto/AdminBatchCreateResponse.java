package com.hocchu.web.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBatchCreateResponse {
    private int successCount;
    private int failureCount;
    private List<String> errors;
}
