package com.lottery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lottery.dto.AnalyzeResultDTO;
import com.lottery.dto.DynamicAnalysisBatchSaveRequestDTO;
import com.lottery.dto.DynamicAnalysisHistoryCompareDTO;
import com.lottery.dto.DynamicAnalysisHistoryCompareRequestDTO;
import com.lottery.dto.DynamicAnalysisSaveRequestDTO;
import com.lottery.dto.DynamicRuleAnalyzeDTO;
import com.lottery.dto.DynamicRuleAnalyzeRequestDTO;
import com.lottery.dto.ImportResultDTO;
import com.lottery.dto.PageResultDTO;
import com.lottery.entity.DynamicAnalysisRecord;
import com.lottery.entity.DynamicRule;
import com.lottery.entity.LotteryRecord;
import com.lottery.service.LotteryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Data", description = "数据导入、规则分析与历史结果管理接口")
public class LotteryController {

    private final LotteryService lotteryService;

    /**
     * 导入 XLS 文件并写入数据库。
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入数据", description = "上传 .xls 文件，批量导入或更新数据。")
    public com.lottery.dto.ApiResponse<ImportResultDTO> importFile(
            @Parameter(description = "数据 XLS 文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            ImportResultDTO result = lotteryService.importXls(file);
            return com.lottery.dto.ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Import failed", e);
            return com.lottery.dto.ApiResponse.error("Import failed: " + e.getMessage());
        }
    }

    /**
     * 分页查询数据记录，支持按日期和起始期号过滤。
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询数据", description = "按查询日期和起始期号分页获取数据记录。")
    public com.lottery.dto.ApiResponse<PageResultDTO<LotteryRecord>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "查询日期，格式 yyyy-MM-dd") @RequestParam(value = "queryDate", required = false) String queryDate,
            @Parameter(description = "起始期号，支持完整期号或日期后缀") @RequestParam(value = "startIssueNo", required = false) String startIssueNo) {
        try {
            Page<LotteryRecord> pageResult = lotteryService.listByPage(page, size, queryDate, startIssueNo);
            PageResultDTO<LotteryRecord> dto = new PageResultDTO<>(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getPages(),
                    pageResult.getRecords()
            );
            return com.lottery.dto.ApiResponse.success(dto);
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 对固定内置规则执行分析。
     */
    @GetMapping("/analyze")
    @Operation(summary = "固定规则分析", description = "基于内置大小规则，对指定日期数据进行命中/未命中分析。")
    public com.lottery.dto.ApiResponse<AnalyzeResultDTO> analyze(
            @Parameter(description = "分析位次，取值 0-10") @RequestParam("n") int n,
            @Parameter(description = "查询日期，格式 yyyy-MM-dd") @RequestParam(value = "queryDate", required = false) String queryDate) {
        try {
            return com.lottery.dto.ApiResponse.success(lotteryService.analyze(n, queryDate));
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 导入动态规则模板文件。
     */
    @PostMapping(value = "/dynamic-rules/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入动态规则", description = "上传动态规则 XLS 文件并覆盖当前规则库。")
    public com.lottery.dto.ApiResponse<List<DynamicRuleAnalyzeDTO.RuleResult>> importDynamicRules(
            @Parameter(description = "动态规则 XLS 文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            return com.lottery.dto.ApiResponse.success(lotteryService.importDynamicRules(file));
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Dynamic rule import failed", e);
            return com.lottery.dto.ApiResponse.error("Import failed: " + e.getMessage());
        }
    }

    @GetMapping("/dynamic-rules")
    @Operation(summary = "查询动态规则", description = "获取数据库中已导入的动态规则列表。")
    public com.lottery.dto.ApiResponse<List<DynamicRule>> listDynamicRules() {
        return com.lottery.dto.ApiResponse.success(lotteryService.listDynamicRules());
    }

    @PostMapping("/dynamic-rules/analyze")
    @Operation(summary = "动态规则分析", description = "使用当前导入的动态规则，对指定日期和位次执行统计分析。")
    public com.lottery.dto.ApiResponse<DynamicRuleAnalyzeDTO> analyzeDynamicRules(
            @Parameter(description = "动态规则分析请求体", required = true) @RequestBody DynamicRuleAnalyzeRequestDTO request) {
        try {
            return com.lottery.dto.ApiResponse.success(lotteryService.analyzeDynamicRules(request));
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/dynamic-rules/save")
    @Operation(summary = "保存动态分析结果", description = "保存当前位次的动态规则分析结果。")
    public com.lottery.dto.ApiResponse<Void> saveDynamicAnalysis(
            @Parameter(description = "动态分析保存请求体", required = true) @RequestBody DynamicAnalysisSaveRequestDTO request) {
        try {
            lotteryService.saveDynamicAnalysis(request);
            return com.lottery.dto.ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/dynamic-rules/save-all")
    @Operation(summary = "一键保存动态分析结果", description = "按 1 到 10 位依次分析并保存当前日期的全部动态规则结果。")
    public com.lottery.dto.ApiResponse<Void> saveAllDynamicAnalysis(
            @Parameter(description = "批量保存请求体", required = true) @RequestBody DynamicAnalysisBatchSaveRequestDTO request) {
        try {
            lotteryService.saveAllDynamicAnalysis(request);
            return com.lottery.dto.ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询动态分析历史记录。
     */
    @GetMapping("/dynamic-analysis-records")
    @Operation(summary = "分页查询动态分析历史", description = "按日期、规则内容、位次和未命中次数排序查询历史动态分析记录。")
    public com.lottery.dto.ApiResponse<PageResultDTO<DynamicAnalysisRecord>> listDynamicAnalysisRecords(
            @Parameter(description = "页码，从 1 开始") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "来源日期，格式 yyyy-MM-dd") @RequestParam(value = "sourceDate", required = false) String sourceDate,
            @Parameter(description = "动态规则关键字") @RequestParam(value = "dynamicRule", required = false) String dynamicRule,
            @Parameter(description = "名次，取值 1-10") @RequestParam(value = "rankNo", required = false) Integer rankNo,
            @Parameter(description = "未命中次数排序，ascending 或 descending") @RequestParam(value = "sortOrder", required = false) String sortOrder) {
        try {
            return com.lottery.dto.ApiResponse.success(
                    lotteryService.listDynamicAnalysisRecords(page, size, sourceDate, dynamicRule, rankNo, sortOrder)
            );
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/dynamic-analysis-records/compare")
    @Operation(summary = "多日期历史对比", description = "对多个来源日期下的动态规则分析结果进行横向比较。")
    public com.lottery.dto.ApiResponse<DynamicAnalysisHistoryCompareDTO> compareDynamicAnalysisRecords(
            @Parameter(description = "历史对比请求体", required = true) @RequestBody DynamicAnalysisHistoryCompareRequestDTO request) {
        try {
            return com.lottery.dto.ApiResponse.success(lotteryService.compareDynamicAnalysisRecords(request));
        } catch (IllegalArgumentException e) {
            return com.lottery.dto.ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空数据", description = "清空数据表中的全部记录。")
    public com.lottery.dto.ApiResponse<Void> clearAll() {
        try {
            lotteryService.clearAll();
            return com.lottery.dto.ApiResponse.success(null);
        } catch (Exception e) {
            log.error("Clear failed", e);
            return com.lottery.dto.ApiResponse.error("Clear failed: " + e.getMessage());
        }
    }

    @PostMapping("/batch-add")
    @Operation(summary = "批量新增或更新数据", description = "支持手工录入或编辑后的数据批量提交。")
    public com.lottery.dto.ApiResponse<ImportResultDTO> batchAdd(
            @Parameter(description = "数据记录列表", required = true) @RequestBody List<LotteryRecord> records) {
        try {
            if (records == null || records.isEmpty()) {
                return com.lottery.dto.ApiResponse.error("数据不能为空");
            }
            return com.lottery.dto.ApiResponse.success(lotteryService.batchAdd(records));
        } catch (Exception e) {
            log.error("Batch add failed", e);
            return com.lottery.dto.ApiResponse.error("Batch add failed: " + e.getMessage());
        }
    }
}
