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
@Schema(description = "固定规则分析结果")
public class AnalyzeResultDTO {
    @Schema(description = "输入的分析位次")
    private int inputN;

    @Schema(description = "实际分析的球列序号")
    private int ballColumn;

    @Schema(description = "本次分析命中的起始期号")
    private String startIssueNo;

    @Schema(description = "规则分析结果列表")
    private List<RuleResult> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单条固定规则分析结果")
    public static class RuleResult {
        @Schema(description = "规则编号")
        private int ruleNo;

        @Schema(description = "连续未命中超过 6 次的统计次数")
        private int totalMissCount;

        @Schema(description = "未命中事件列表")
        private List<MissEvent> missEvents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "未命中事件")
    public static class MissEvent {
        @Schema(description = "前端展示使用的期号")
        private String displayIssueNo;

        @Schema(description = "实际命中的起始期号")
        private String actualIssueNo;

        @Schema(description = "是否为伪命中")
        private boolean pseudoHit;
    }
}
