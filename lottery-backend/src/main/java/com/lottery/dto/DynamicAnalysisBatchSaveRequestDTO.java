package com.lottery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "动态分析批量保存请求参数")
public class DynamicAnalysisBatchSaveRequestDTO {
    @Schema(description = "查询日期，格式 yyyy-MM-dd", example = "2026-04-22")
    private String queryDate;
}
