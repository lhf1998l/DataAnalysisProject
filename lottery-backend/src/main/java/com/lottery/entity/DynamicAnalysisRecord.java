package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dynamic_analysis_record")
@Schema(description = "动态规则分析历史记录")
public class DynamicAnalysisRecord {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("source_date")
    @Schema(description = "来源日期，格式 yyyyMMdd")
    private String sourceDate;

    @TableField("rank_no")
    @Schema(description = "名次")
    private Integer rankNo;

    @TableField("dynamic_rule")
    @Schema(description = "动态规则内容")
    private String dynamicRule;

    @TableField("total_miss_count")
    @Schema(description = "未命中统计次数")
    private Integer totalMissCount;

    @TableField("issue_nos")
    @Schema(description = "展示期号列表")
    private String issueNos;

    @TableField("actual_issue_nos")
    @Schema(description = "实际期号列表")
    private String actualIssueNos;

    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
