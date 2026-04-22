package com.lottery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "动态分析历史对比请求参数")
public class DynamicAnalysisHistoryCompareRequestDTO {
    @Schema(description = "待比较的来源日期列表", example = "[\"2026-04-20\",\"2026-04-21\"]")
    private List<String> sourceDates;

    @Schema(description = "对比的名次", example = "1")
    private Integer rankNo;

    @Schema(description = "按规则关键字过滤", example = "小大大小")
    private String dynamicRule;
}
