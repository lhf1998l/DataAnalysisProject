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
@Schema(description = "动态规则分析结果")
public class DynamicRuleAnalyzeDTO {
    @Schema(description = "输入的分析位次")
    private int inputN;

    @Schema(description = "实际分析的名次")
    private int ballColumn;

    @Schema(description = "分析起始期号")
    private String startIssueNo;

    @Schema(description = "动态规则统计结果")
    private List<RuleResult> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单条动态规则分析结果")
    public static class RuleResult {
        @Schema(description = "规则编号")
        private int ruleNo;

        @Schema(description = "导入的规则内容")
        private String importedRule;

        @Schema(description = "连续未命中超过 6 次的统计次数")
        private int totalMissCount;

        @Schema(description = "未命中事件列表")
        private List<AnalyzeResultDTO.MissEvent> missEvents;
    }
}
