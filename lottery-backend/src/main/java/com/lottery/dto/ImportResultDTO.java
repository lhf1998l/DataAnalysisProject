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
@Schema(description = "导入结果统计")
public class ImportResultDTO {
    @Schema(description = "处理总行数")
    private int totalRows;

    @Schema(description = "新增条数")
    private int insertedCount;

    @Schema(description = "更新条数")
    private int updatedCount;

    @Schema(description = "跳过条数")
    private int skippedCount;

    @Schema(description = "错误消息列表")
    private List<String> errorMessages;
}
