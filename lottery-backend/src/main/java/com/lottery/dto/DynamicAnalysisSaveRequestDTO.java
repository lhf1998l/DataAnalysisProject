package com.lottery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "动态分析结果保存请求参数")
public class DynamicAnalysisSaveRequestDTO {
    @Schema(description = "查询日期，格式 yyyy-MM-dd", example = "2026-04-22")
    private String queryDate;

    @Schema(description = "保存的名次", example = "1")
    private Integer rankNo;

    @Schema(description = "待保存的规则分析结果")
    private List<DynamicRuleAnalyzeDTO.RuleResult> rules;
}
