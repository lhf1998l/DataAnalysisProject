package com.lottery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.lottery.mapper.DynamicAnalysisRecordMapper;
import com.lottery.mapper.DynamicRuleMapper;
import com.lottery.mapper.LotteryMapper;
import com.lottery.service.LotteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryServiceImpl extends ServiceImpl<LotteryMapper, LotteryRecord> implements LotteryService {

    private static final boolean[][] RULES = {
            {false, true, true, false, false, false, true, false, true, true},
            {false, true, true, false, true, true, false, false, true, true},
            {true, false, false, true, true, true, false, true, false, false},
            {true, false, false, true, false, false, true, true, false, false}
    };
    private static final DateTimeFormatter QUERY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LotteryMapper lotteryMapper;
    private final DynamicRuleMapper dynamicRuleMapper;
    private final DynamicAnalysisRecordMapper dynamicAnalysisRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 解析开奖 XLS 文件，并按期号做新增或更新。
     */
    public ImportResultDTO importXls(MultipartFile file) {
        int totalRows = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<String> errorMessages = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new HSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            log.info("Sheet name: {}, Rows: {}", sheet.getSheetName(), sheet.getLastRowNum());

            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Cell issueCell = row.getCell(1);
                if (issueCell == null) {
                    continue;
                }
                String issueNo = getCellStringValue(issueCell).trim();
                if (issueNo.isEmpty() || issueNo.equals("期数") || issueNo.equals("期号")) {
                    continue;
                }

                totalRows++;

                Cell numbersCell = row.getCell(2);
                if (numbersCell == null) {
                    skippedCount++;
                    continue;
                }

                String rawNumbers = getCellStringValue(numbersCell).trim();
                String[] parts = rawNumbers.replace("，", ",").split(",");
                if (parts.length != 10) {
                    log.warn("Row {}, skipped because parts.length is {} (expected 10). rawNumbers='{}'", rowIndex, parts.length, rawNumbers);
                    skippedCount++;
                    continue;
                }

                LotteryRecord record = new LotteryRecord();
                record.setIssueNo(issueNo);
                record.setBall1(Integer.parseInt(parts[0].trim()));
                record.setBall2(Integer.parseInt(parts[1].trim()));
                record.setBall3(Integer.parseInt(parts[2].trim()));
                record.setBall4(Integer.parseInt(parts[3].trim()));
                record.setBall5(Integer.parseInt(parts[4].trim()));
                record.setBall6(Integer.parseInt(parts[5].trim()));
                record.setBall7(Integer.parseInt(parts[6].trim()));
                record.setBall8(Integer.parseInt(parts[7].trim()));
                record.setBall9(Integer.parseInt(parts[8].trim()));
                record.setBall10(Integer.parseInt(parts[9].trim()));
                record.setRawNumbers(rawNumbers);

                boolean exists = lotteryMapper.selectCount(
                        new LambdaQueryWrapper<LotteryRecord>().eq(LotteryRecord::getIssueNo, issueNo)
                ) > 0;
                lotteryMapper.insertOrUpdate(record);

                if (exists) {
                    updatedCount++;
                } else {
                    insertedCount++;
                }
            }
        } catch (Exception e) {
            log.error("Import error", e);
            errorMessages.add("Error: " + e.getMessage());
        }

        return ImportResultDTO.builder()
                .totalRows(totalRows)
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .errorMessages(errorMessages)
                .build();
    }

    @Override
    public Page<LotteryRecord> listByPage(int pageNum, int pageSize, String queryDate, String startIssueNo) {
        String normalizedStartIssueNo = normalizeStartIssueNo(startIssueNo, queryDate);
        if (normalizedStartIssueNo != null) {
            List<LotteryRecord> records = lotteryMapper.selectList(new LambdaQueryWrapper<LotteryRecord>()
                    .likeRight(LotteryRecord::getIssueNo, resolveIssueDatePrefix(queryDate))
                    .ge(LotteryRecord::getIssueNo, normalizedStartIssueNo)
                    .last("ORDER BY CAST(issue_no AS UNSIGNED) ASC LIMIT 20"));

            Page<LotteryRecord> page = new Page<>(1, 20, false);
            page.setRecords(records);
            page.setTotal(records.size());
            page.setPages(records.isEmpty() ? 0 : 1);
            return page;
        }

        Page<LotteryRecord> page = new Page<>(pageNum, pageSize);
        return lotteryMapper.selectPage(page, buildQueryWrapper(queryDate, false));
    }

    @Override
    /**
     * 按内置规则执行固定分析，返回指定名次的统计结果。
     */
    public AnalyzeResultDTO analyze(int n, String queryDate) {
        AnalysisContext context = buildAnalysisContext(n, queryDate);
        if (context == null) {
            return null;
        }

        List<AnalyzeResultDTO.RuleResult> ruleResults = new ArrayList<>();
        for (int ruleIndex = 0; ruleIndex < RULES.length; ruleIndex++) {
            AnalysisResult result = calculateMissStats(context.records, context.ballColumn, RULES[ruleIndex]);
            ruleResults.add(new AnalyzeResultDTO.RuleResult(ruleIndex + 1, result.getTotalMissCount(), result.getMissEvents()));
        }

        return AnalyzeResultDTO.builder()
                .inputN(n)
                .ballColumn(context.ballColumn)
                .startIssueNo(context.startIssueNo)
                .rules(ruleResults)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 解析动态规则文件并覆盖当前规则表。
     */
    public List<DynamicRuleAnalyzeDTO.RuleResult> importDynamicRules(MultipartFile file) {
        List<boolean[]> rulePatterns = parseDynamicRulePatterns(file);
        dynamicRuleMapper.delete(new LambdaQueryWrapper<>());

        List<DynamicRuleAnalyzeDTO.RuleResult> ruleResults = new ArrayList<>();
        for (int i = 0; i < rulePatterns.size(); i++) {
            DynamicRule dynamicRule = new DynamicRule();
            dynamicRule.setRuleNo(i + 1);
            dynamicRule.setRulePattern(formatRule(rulePatterns.get(i)));
            dynamicRuleMapper.insert(dynamicRule);

            ruleResults.add(DynamicRuleAnalyzeDTO.RuleResult.builder()
                    .ruleNo(i + 1)
                    .importedRule(formatRule(rulePatterns.get(i)))
                    .totalMissCount(0)
                    .missEvents(Collections.emptyList())
                    .build());
        }
        return ruleResults;
    }

    @Override
    public List<DynamicRule> listDynamicRules() {
        return dynamicRuleMapper.selectList(new LambdaQueryWrapper<DynamicRule>()
                .orderByAsc(DynamicRule::getRuleNo));
    }

    @Override
    /**
     * 基于当前已导入的动态规则执行分析。
     */
    public DynamicRuleAnalyzeDTO analyzeDynamicRules(DynamicRuleAnalyzeRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        List<DynamicRule> dynamicRules = listDynamicRules();
        if (dynamicRules.isEmpty()) {
            throw new IllegalArgumentException("请先导入规则");
        }

        AnalysisContext context = buildAnalysisContext(request.getN(), request.getQueryDate());
        if (context == null) {
            return null;
        }

        List<DynamicRuleAnalyzeDTO.RuleResult> ruleResults = new ArrayList<>();
        for (DynamicRule dynamicRule : dynamicRules) {
            boolean[] pattern = normalizeRulePattern(dynamicRule.getRulePattern());
            if (pattern == null) {
                throw new IllegalArgumentException("存在无法识别的规则：" + dynamicRule.getRulePattern());
            }

            AnalysisResult result = calculateMissStats(context.records, context.ballColumn, pattern);
            ruleResults.add(DynamicRuleAnalyzeDTO.RuleResult.builder()
                    .ruleNo(dynamicRule.getRuleNo())
                    .importedRule(formatRule(pattern))
                    .totalMissCount(result.getTotalMissCount())
                    .missEvents(result.getMissEvents())
                    .build());
        }

        return DynamicRuleAnalyzeDTO.builder()
                .inputN(request.getN())
                .ballColumn(context.ballColumn)
                .startIssueNo(context.startIssueNo)
                .rules(ruleResults)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 保存单个名次的动态分析结果，保存前先清除同日期同名次历史数据。
     */
    public void saveDynamicAnalysis(DynamicAnalysisSaveRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (request.getRules() == null || request.getRules().isEmpty()) {
            throw new IllegalArgumentException("请先执行变动规则分析");
        }
        if (request.getRankNo() == null || request.getRankNo() < 1 || request.getRankNo() > 10) {
            throw new IllegalArgumentException("名次必须在 1 到 10 之间");
        }

        String sourceDate = resolveIssueDatePrefix(request.getQueryDate());
        dynamicAnalysisRecordMapper.delete(new LambdaQueryWrapper<DynamicAnalysisRecord>()
                .eq(DynamicAnalysisRecord::getSourceDate, sourceDate)
                .eq(DynamicAnalysisRecord::getRankNo, request.getRankNo()));

        for (DynamicRuleAnalyzeDTO.RuleResult rule : request.getRules()) {
            DynamicAnalysisRecord record = new DynamicAnalysisRecord();
            record.setSourceDate(sourceDate);
            record.setRankNo(request.getRankNo());
            record.setDynamicRule(rule.getImportedRule());
            record.setTotalMissCount(rule.getTotalMissCount());
            record.setIssueNos(joinIssueNos(rule.getMissEvents()));
            record.setActualIssueNos(joinActualIssueNos(rule.getMissEvents()));
            dynamicAnalysisRecordMapper.insert(record);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 针对 1 到 10 名逐一分析并保存动态规则结果。
     */
    public void saveAllDynamicAnalysis(DynamicAnalysisBatchSaveRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        String queryDate = request.getQueryDate();
        String sourceDate = resolveIssueDatePrefix(queryDate);
        dynamicAnalysisRecordMapper.delete(new LambdaQueryWrapper<DynamicAnalysisRecord>()
                .eq(DynamicAnalysisRecord::getSourceDate, sourceDate));

        for (int n = 1; n <= 10; n++) {
            DynamicRuleAnalyzeDTO analyzeResult = analyzeDynamicRules(buildDynamicAnalyzeRequest(n, queryDate));
            if (analyzeResult == null || analyzeResult.getRules() == null || analyzeResult.getRules().isEmpty()) {
                continue;
            }

            DynamicAnalysisSaveRequestDTO saveRequest = new DynamicAnalysisSaveRequestDTO();
            saveRequest.setQueryDate(queryDate);
            saveRequest.setRankNo(analyzeResult.getBallColumn());
            saveRequest.setRules(analyzeResult.getRules());
            saveDynamicAnalysis(saveRequest);
        }
    }

    @Override
    public PageResultDTO<DynamicAnalysisRecord> listDynamicAnalysisRecords(
            int pageNum, int pageSize, String sourceDate, String issueNo, String dynamicRule, Integer rankNo, String sortOrder) {
        Page<DynamicAnalysisRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DynamicAnalysisRecord> wrapper = Wrappers.lambdaQuery();

        if (sourceDate != null && !sourceDate.trim().isEmpty()) {
            wrapper.eq(DynamicAnalysisRecord::getSourceDate, resolveIssueDatePrefix(sourceDate));
        }
        if (issueNo != null && !issueNo.trim().isEmpty()) {
            wrapper.like(DynamicAnalysisRecord::getIssueNos, normalizeHistoryIssueNo(issueNo, sourceDate));
        }
        if (dynamicRule != null && !dynamicRule.trim().isEmpty()) {
            wrapper.like(DynamicAnalysisRecord::getDynamicRule, dynamicRule.trim());
        }
        if (rankNo != null) {
            if (rankNo < 1 || rankNo > 10) {
                throw new IllegalArgumentException("名次必须在 1 到 10 之间");
            }
            wrapper.eq(DynamicAnalysisRecord::getRankNo, rankNo);
        }

        if ("ascending".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(DynamicAnalysisRecord::getTotalMissCount);
        } else if ("descending".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByDesc(DynamicAnalysisRecord::getTotalMissCount);
        }

        wrapper.orderByDesc(DynamicAnalysisRecord::getSourceDate)
                .orderByAsc(DynamicAnalysisRecord::getRankNo)
                .orderByAsc(DynamicAnalysisRecord::getId);

        Page<DynamicAnalysisRecord> result = dynamicAnalysisRecordMapper.selectPage(page, wrapper);
        return new PageResultDTO<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getRecords()
        );
    }

    @Override
    public DynamicAnalysisHistoryCompareDTO compareDynamicAnalysisRecords(DynamicAnalysisHistoryCompareRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (request.getSourceDates() == null || request.getSourceDates().isEmpty()) {
            throw new IllegalArgumentException("请选择至少一个日期");
        }
        if (request.getRankNo() == null || request.getRankNo() < 1 || request.getRankNo() > 10) {
            throw new IllegalArgumentException("名次必须在 1 到 10 之间");
        }

        List<String> normalizedDates = request.getSourceDates().stream()
                .map(this::resolveIssueDatePrefix)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<DynamicAnalysisRecord> compareWrapper = Wrappers.<DynamicAnalysisRecord>lambdaQuery()
                .in(DynamicAnalysisRecord::getSourceDate, normalizedDates)
                .eq(DynamicAnalysisRecord::getRankNo, request.getRankNo());
        if (request.getDynamicRule() != null && !request.getDynamicRule().trim().isEmpty()) {
            compareWrapper.like(DynamicAnalysisRecord::getDynamicRule, request.getDynamicRule().trim());
        }
        compareWrapper.orderByAsc(DynamicAnalysisRecord::getDynamicRule)
                .orderByAsc(DynamicAnalysisRecord::getSourceDate);

        List<DynamicAnalysisRecord> records = dynamicAnalysisRecordMapper.selectList(compareWrapper);

        Map<String, List<DynamicAnalysisRecord>> groupedRecords = new LinkedHashMap<>();
        for (DynamicAnalysisRecord record : records) {
            groupedRecords.computeIfAbsent(record.getDynamicRule(), key -> new ArrayList<>()).add(record);
        }

        List<DynamicAnalysisHistoryCompareDTO.RuleCompare> ruleCompares = new ArrayList<>();
        for (Map.Entry<String, List<DynamicAnalysisRecord>> entry : groupedRecords.entrySet()) {
            List<DynamicAnalysisHistoryCompareDTO.CompareItem> items = entry.getValue().stream()
                    .map(record -> DynamicAnalysisHistoryCompareDTO.CompareItem.builder()
                            .sourceDate(record.getSourceDate())
                            .totalMissCount(record.getTotalMissCount())
                            .issueNos(record.getIssueNos())
                            .actualIssueNos(record.getActualIssueNos())
                            .build())
                    .collect(Collectors.toList());

            int totalMissCountSum = entry.getValue().stream()
                    .map(DynamicAnalysisRecord::getTotalMissCount)
                    .filter(count -> count != null)
                    .mapToInt(Integer::intValue)
                    .sum();

            ruleCompares.add(DynamicAnalysisHistoryCompareDTO.RuleCompare.builder()
                    .dynamicRule(entry.getKey())
                    .totalMissCountSum(totalMissCountSum)
                    .items(items)
                    .build());
        }

        return DynamicAnalysisHistoryCompareDTO.builder()
                .rankNo(request.getRankNo())
                .ruleCompares(ruleCompares)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll() {
        this.remove(new LambdaQueryWrapper<LotteryRecord>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultDTO batchAdd(List<LotteryRecord> records) {
        int insertedCount = 0;
        int updatedCount = 0;
        for (LotteryRecord record : records) {
            boolean exists = lotteryMapper.selectCount(
                    new LambdaQueryWrapper<LotteryRecord>().eq(LotteryRecord::getIssueNo, record.getIssueNo())
            ) > 0;

            String raw = String.format("%02d,%02d,%02d,%02d,%02d,%02d,%02d,%02d,%02d,%02d",
                    record.getBall1(), record.getBall2(), record.getBall3(), record.getBall4(), record.getBall5(),
                    record.getBall6(), record.getBall7(), record.getBall8(), record.getBall9(), record.getBall10());
            record.setRawNumbers(raw);

            lotteryMapper.insertOrUpdate(record);
            if (exists) {
                updatedCount++;
            } else {
                insertedCount++;
            }
        }
        return ImportResultDTO.builder()
                .totalRows(records.size())
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .build();
    }

    /**
     * 计算分析起点、目标名次以及用于后续计算的记录集合。
     */
    private AnalysisContext buildAnalysisContext(int n, String queryDate) {
        int normalizedN = normalizeInputN(n);
        int ballColumn = resolveBallColumn(normalizedN);
        int targetTail = (normalizedN + 1) % 10;

        List<LotteryRecord> all = lotteryMapper.selectList(buildQueryWrapper(queryDate, true));
        if (all.isEmpty()) {
            return null;
        }

        int startIndex = -1;
        for (int i = 0; i < all.size(); i++) {
            String issue = all.get(i).getIssueNo();
            int tail = Integer.parseInt(issue.substring(issue.length() - 1));
            if (tail == targetTail) {
                startIndex = i;
                break;
            }
        }

        if (startIndex == -1) {
            return null;
        }

        return new AnalysisContext(ballColumn, all.get(startIndex).getIssueNo(), all.subList(startIndex, all.size()));
    }

    /**
     * 按 10 条记录为一组计算连续未命中统计。
     */
    private AnalysisResult calculateMissStats(List<LotteryRecord> sourceRecords, int ballColumn, boolean[] pattern) {
        int totalMissEvents = 0;
        List<AnalyzeResultDTO.MissEvent> missEvents = new ArrayList<>();

        for (int groupStart = 0; groupStart < sourceRecords.size(); groupStart += 10) {
            int groupEnd = Math.min(groupStart + 10, sourceRecords.size());
            List<LotteryRecord> group = sourceRecords.subList(groupStart, groupEnd);

            int currentMissCount = 0;
            boolean groupHasRecorded = false;

            for (int i = 0; i < group.size() && i < pattern.length; i++) {
                LotteryRecord record = group.get(i);
                int ballValue = getBallValue(record, ballColumn);
                boolean isBig = ballValue >= 6;
                boolean isHit = isBig == pattern[i];

                if (isHit) {
                    currentMissCount = 0;
                    continue;
                }

                currentMissCount++;
                if (currentMissCount >= 7 && !groupHasRecorded) {
                    int actualStartIndex = i - currentMissCount + 1;
                    String displayIssueNo = group.get(0).getIssueNo();
                    String actualIssueNo = group.get(actualStartIndex).getIssueNo();

                    totalMissEvents++;
                    missEvents.add(AnalyzeResultDTO.MissEvent.builder()
                            .displayIssueNo(displayIssueNo)
                            .actualIssueNo(actualIssueNo)
                            .pseudoHit(actualStartIndex > 0)
                            .build());
                    groupHasRecorded = true;
                }
            }
        }

        return new AnalysisResult(totalMissEvents, missEvents);
    }

    /**
     * 从 XLS 文件中提取动态规则模式。
     */
    private List<boolean[]> parseDynamicRulePatterns(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("规则文件不能为空");
        }

        List<boolean[]> rulePatterns = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new HSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                List<String> cells = extractRowCells(row);
                if (cells.isEmpty() || isRuleHeaderRow(cells)) {
                    continue;
                }

                boolean[] pattern = resolveRulePattern(cells);
                if (pattern != null) {
                    rulePatterns.add(pattern);
                }
            }
        } catch (Exception e) {
            log.error("Dynamic rule import error", e);
            throw new IllegalArgumentException("规则文件解析失败: " + e.getMessage());
        }

        if (rulePatterns.isEmpty()) {
            throw new IllegalArgumentException("未从文件中识别到有效规则");
        }

        return rulePatterns;
    }

    private List<String> extractRowCells(Row row) {
        List<String> cells = new ArrayList<>();
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return cells;
        }

        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            String cellValue = getCellStringValue(row.getCell(cellIndex)).trim();
            if (!cellValue.isEmpty()) {
                cells.add(cellValue);
            }
        }
        return cells;
    }

    private boolean isRuleHeaderRow(List<String> cells) {
        String joined = String.join("", cells);
        return joined.contains("规则")
                || joined.contains("序号")
                || joined.contains("未命中")
                || joined.contains("导入");
    }

    private boolean[] resolveRulePattern(List<String> cells) {
        List<Boolean> tokenPattern = new ArrayList<>();
        for (String cell : cells) {
            Boolean token = normalizeSingleRuleToken(cell);
            if (token != null) {
                tokenPattern.add(token);
            }
        }
        if (tokenPattern.size() >= 10) {
            return toRulePattern(tokenPattern.subList(0, 10));
        }

        for (String cell : cells) {
            boolean[] pattern = normalizeRulePattern(cell);
            if (pattern != null) {
                return pattern;
            }
        }

        return normalizeRulePattern(String.join("", cells));
    }

    private boolean[] normalizeRulePattern(String source) {
        if (source == null) {
            return null;
        }

        List<Boolean> pattern = new ArrayList<>();
        String upper = source.toUpperCase(Locale.ROOT);
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            if (current == '大' || current == 'B') {
                pattern.add(Boolean.TRUE);
                continue;
            }
            if (current == '小' || current == 'S') {
                pattern.add(Boolean.FALSE);
                continue;
            }
            if (current == '1') {
                pattern.add(Boolean.TRUE);
                continue;
            }
            if (current == '0') {
                pattern.add(Boolean.FALSE);
                continue;
            }
            if (upper.startsWith("TRUE", i)) {
                pattern.add(Boolean.TRUE);
                i += 3;
                continue;
            }
            if (upper.startsWith("FALSE", i)) {
                pattern.add(Boolean.FALSE);
                i += 4;
            }
        }

        if (pattern.size() < 10) {
            return null;
        }

        return toRulePattern(pattern.subList(0, 10));
    }

    private Boolean normalizeSingleRuleToken(String cell) {
        if (cell == null) {
            return null;
        }

        String value = cell.trim().toUpperCase(Locale.ROOT);
        if ("大".equals(value) || "1".equals(value) || "B".equals(value) || "TRUE".equals(value)) {
            return Boolean.TRUE;
        }
        if ("小".equals(value) || "0".equals(value) || "S".equals(value) || "FALSE".equals(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean[] toRulePattern(List<Boolean> values) {
        boolean[] pattern = new boolean[10];
        for (int i = 0; i < 10; i++) {
            pattern[i] = values.get(i);
        }
        return pattern;
    }

    private String formatRule(boolean[] pattern) {
        StringBuilder builder = new StringBuilder(pattern.length);
        for (boolean value : pattern) {
            builder.append(value ? '大' : '小');
        }
        return builder.toString();
    }

    private String joinIssueNos(List<AnalyzeResultDTO.MissEvent> missEvents) {
        if (missEvents == null || missEvents.isEmpty()) {
            return "";
        }

        return missEvents.stream()
                .map(AnalyzeResultDTO.MissEvent::getDisplayIssueNo)
                .collect(Collectors.joining(","));
    }

    private String joinActualIssueNos(List<AnalyzeResultDTO.MissEvent> missEvents) {
        if (missEvents == null || missEvents.isEmpty()) {
            return "";
        }

        return missEvents.stream()
                .map(AnalyzeResultDTO.MissEvent::getActualIssueNo)
                .collect(Collectors.joining(","));
    }

    private DynamicRuleAnalyzeRequestDTO buildDynamicAnalyzeRequest(int n, String queryDate) {
        DynamicRuleAnalyzeRequestDTO request = new DynamicRuleAnalyzeRequestDTO();
        request.setN(n);
        request.setQueryDate(queryDate);
        return request;
    }

    private LambdaQueryWrapper<LotteryRecord> buildQueryWrapper(String queryDate, boolean ascending) {
        return new LambdaQueryWrapper<LotteryRecord>()
                .likeRight(LotteryRecord::getIssueNo, resolveIssueDatePrefix(queryDate))
                .last("ORDER BY CAST(issue_no AS UNSIGNED) " + (ascending ? "ASC" : "DESC"));
    }

    private String normalizeStartIssueNo(String startIssueNo, String queryDate) {
        if (startIssueNo == null || startIssueNo.trim().isEmpty()) {
            return null;
        }

        String trimmed = startIssueNo.trim();
        if (!trimmed.matches("\\d+")) {
            throw new IllegalArgumentException("startIssueNo must be numeric");
        }

        if (trimmed.length() >= 8) {
            return trimmed;
        }

        return resolveIssueDatePrefix(queryDate) + trimmed;
    }

    private String normalizeHistoryIssueNo(String issueNo, String sourceDate) {
        if (issueNo == null || issueNo.trim().isEmpty()) {
            return null;
        }

        String trimmed = issueNo.trim();
        if (!trimmed.matches("\\d+")) {
            throw new IllegalArgumentException("issueNo must be numeric");
        }

        if (trimmed.length() >= 8) {
            return trimmed;
        }

        if (sourceDate == null || sourceDate.trim().isEmpty()) {
            return trimmed;
        }

        return resolveIssueDatePrefix(sourceDate) + trimmed;
    }

    /**
     * 将前端日期格式统一转换为期号使用的 yyyyMMdd 前缀。
     */
    private String resolveIssueDatePrefix(String queryDate) {
        if (queryDate == null || queryDate.trim().isEmpty()) {
            return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        }

        try {
            return LocalDate.parse(queryDate.trim(), QUERY_DATE_FORMATTER)
                    .format(DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("queryDate must be yyyy-MM-dd");
        }
    }

    private int normalizeInputN(int n) {
        if (n >= 1 && n <= 10) {
            return n == 10 ? 0 : n;
        }
        if (n == 0) {
            return 0;
        }
        if (n > 10) {
            return n % 10;
        }
        throw new IllegalArgumentException("n must be in [1,10] or a full issue number");
    }

    private int resolveBallColumn(int normalizedN) {
        return normalizedN == 0 ? 10 : normalizedN;
    }

    private int getBallValue(LotteryRecord rec, int ballColumn) {
        switch (ballColumn) {
            case 1:
                return rec.getBall1();
            case 2:
                return rec.getBall2();
            case 3:
                return rec.getBall3();
            case 4:
                return rec.getBall4();
            case 5:
                return rec.getBall5();
            case 6:
                return rec.getBall6();
            case 7:
                return rec.getBall7();
            case 8:
                return rec.getBall8();
            case 9:
                return rec.getBall9();
            case 10:
                return rec.getBall10();
            default:
                throw new IllegalArgumentException("Invalid ball column: " + ballColumn);
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString();
    }

    private static class AnalysisContext {
        private final int ballColumn;
        private final String startIssueNo;
        private final List<LotteryRecord> records;

        private AnalysisContext(int ballColumn, String startIssueNo, List<LotteryRecord> records) {
            this.ballColumn = ballColumn;
            this.startIssueNo = startIssueNo;
            this.records = records;
        }
    }

    private static class AnalysisResult {
        private final int totalMissCount;
        private final List<AnalyzeResultDTO.MissEvent> missEvents;

        private AnalysisResult(int totalMissCount, List<AnalyzeResultDTO.MissEvent> missEvents) {
            this.totalMissCount = totalMissCount;
            this.missEvents = missEvents;
        }

        private int getTotalMissCount() {
            return totalMissCount;
        }

        private List<AnalyzeResultDTO.MissEvent> getMissEvents() {
            return missEvents;
        }
    }
}
