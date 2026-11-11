package com.hocchu.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeRequest {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.0")
    @jakarta.validation.constraints.DecimalMax("10.0")
    private Float score;
    @jakarta.validation.constraints.Size(max=4000)
    private String feedback;
}
