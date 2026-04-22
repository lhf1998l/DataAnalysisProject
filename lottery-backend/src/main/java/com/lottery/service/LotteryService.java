package com.lottery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据导入与规则分析服务。
 */
public interface LotteryService extends IService<LotteryRecord> {
    /**
     * 导入 XLS 数据文件。
     */
    ImportResultDTO importXls(MultipartFile file);

    /**
     * 分页查询数据记录。
     */
    Page<LotteryRecord> listByPage(int pageNum, int pageSize, String queryDate, String startIssueNo);

    /**
     * 执行固定规则分析。
     */
    AnalyzeResultDTO analyze(int n, String queryDate);

    /**
     * 导入动态规则。
     */
    List<DynamicRuleAnalyzeDTO.RuleResult> importDynamicRules(MultipartFile file);

    /**
     * 查询动态规则列表。
     */
    List<DynamicRule> listDynamicRules();

    /**
     * 执行动态规则分析。
     */
    DynamicRuleAnalyzeDTO analyzeDynamicRules(DynamicRuleAnalyzeRequestDTO request);

    /**
     * 保存单个名次的动态分析结果。
     */
    void saveDynamicAnalysis(DynamicAnalysisSaveRequestDTO request);

    /**
     * 保存当前日期全部名次的动态分析结果。
     */
    void saveAllDynamicAnalysis(DynamicAnalysisBatchSaveRequestDTO request);

    /**
     * 分页查询动态分析历史。
     */
    PageResultDTO<DynamicAnalysisRecord> listDynamicAnalysisRecords(
            int pageNum, int pageSize, String sourceDate, String dynamicRule, Integer rankNo, String sortOrder);

    /**
     * 比较多个日期的动态分析结果。
     */
    DynamicAnalysisHistoryCompareDTO compareDynamicAnalysisRecords(DynamicAnalysisHistoryCompareRequestDTO request);

    /**
     * 清空数据记录。
     */
    void clearAll();

    /**
     * 批量新增或更新数据记录。
     */
    ImportResultDTO batchAdd(List<LotteryRecord> records);
}
