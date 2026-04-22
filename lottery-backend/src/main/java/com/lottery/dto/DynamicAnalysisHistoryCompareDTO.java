package com.lottery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "动态分析历史对比结果")
public class DynamicAnalysisHistoryCompareDTO {
    @Schema(description = "对比名次")
    private Integer rankNo;

    @Schema(description = "规则对比结果列表")
    private List<RuleCompare> ruleCompares;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单条规则在多日期下的对比结果")
    public static class RuleCompare {
        @Schema(description = "动态规则内容")
        private String dynamicRule;

        @Schema(description = "多日期累计未命中总数")
        private Integer totalMissCountSum;

        @Schema(description = "各日期明细")
        private List<CompareItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单个日期下的规则对比明细")
    public static class CompareItem {
        @Schema(description = "来源日期")
        private String sourceDate;

        @Schema(description = "未命中次数")
        private Integer totalMissCount;

        @Schema(description = "展示期号列表")
        private String issueNos;

        @Schema(description = "实际期号列表")
        private String actualIssueNos;
    }
}
