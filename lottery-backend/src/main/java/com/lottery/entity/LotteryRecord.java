package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lottery_record")
@Schema(description = "数据记录")
public class LotteryRecord {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("issue_no")
    @Schema(description = "期号")
    private String issueNo;

    @TableField("ball_1")
    @Schema(description = "第 1 名号码")
    private Integer ball1;

    @TableField("ball_2")
    @Schema(description = "第 2 名号码")
    private Integer ball2;

    @TableField("ball_3")
    @Schema(description = "第 3 名号码")
    private Integer ball3;

    @TableField("ball_4")
    @Schema(description = "第 4 名号码")
    private Integer ball4;

    @TableField("ball_5")
    @Schema(description = "第 5 名号码")
    private Integer ball5;

    @TableField("ball_6")
    @Schema(description = "第 6 名号码")
    private Integer ball6;

    @TableField("ball_7")
    @Schema(description = "第 7 名号码")
    private Integer ball7;

    @TableField("ball_8")
    @Schema(description = "第 8 名号码")
    private Integer ball8;

    @TableField("ball_9")
    @Schema(description = "第 9 名号码")
    private Integer ball9;

    @TableField("ball_10")
    @Schema(description = "第 10 名号码")
    private Integer ball10;

    @TableField("raw_numbers")
    @Schema(description = "原始号码串")
    private String rawNumbers;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
