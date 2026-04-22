package com.lottery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "动态规则分析请求参数")
public class DynamicRuleAnalyzeRequestDTO {
    @Schema(description = "分析位次，支持 0-10", example = "1")
    private int n;

    @Schema(description = "查询日期，格式 yyyy-MM-dd", example = "2026-04-22")
    private String queryDate;

    @Schema(description = "预留字段，当前以后端已导入规则为准")
    private List<String> rules;
}
