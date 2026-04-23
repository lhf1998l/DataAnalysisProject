<template>
  <div class="app-shell">
    <el-container class="layout-container">
      <el-aside class="sidebar" width="220px">
        <div class="brand-block">
          <div class="brand-title">数据报表系统</div>
          <div class="brand-subtitle">规则分析工作台</div>
        </div>

        <el-menu :default-active="activeTab" class="nav-menu" @select="handleTabChange">
          <el-menu-item index="inherent">
            <el-icon><DataAnalysis /></el-icon>
            <span>固有规则分析</span>
          </el-menu-item>
          <el-menu-item index="dynamic">
            <el-icon><TrendCharts /></el-icon>
            <span>变动规则分析</span>
          </el-menu-item>
          <el-menu-item index="history">
            <el-icon><Tickets /></el-icon>
            <span>变动规则历史分析</span>
          </el-menu-item>
          <el-menu-item index="compare">
            <el-icon><Operation /></el-icon>
            <span>多日期比较分析</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-panel">
        <section v-if="activeTab === 'inherent'">
          <div class="page-header">
            <div>
              <h2 class="page-title">固有规则分析</h2>
              <p class="page-subtitle">导入数据、查询记录并执行现有规则分析。</p>
            </div>
          </div>

          <el-row class="toolbar-row" :gutter="16">
            <el-col :span="24">
              <el-card shadow="hover">
                <div class="toolbar-content">
                  <div class="filter-area">
                    <span class="label">查询日期：</span>
                    <el-date-picker
                      v-model="searchDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="请选择日期"
                      :clearable="false"
                      size="small"
                      @change="handleDateChange"
                    />
                    <span class="label">期号：</span>
                    <el-input
                      v-model.trim="issueSearch"
                      placeholder="输入起始期号"
                      size="small"
                      clearable
                      style="width: 180px"
                      @keyup.enter="handleSearch"
                    />
                    <el-button size="small" type="primary" @click="handleSearch">查询</el-button>
                    <el-button size="small" @click="resetToToday">今天</el-button>
                  </div>

                  <el-divider direction="vertical" />

                  <div class="upload-area">
                    <el-upload
                      :action="importUrl"
                      name="file"
                      accept=".xls"
                      :show-file-list="false"
                      :before-upload="beforeUpload"
                      :on-success="onUploadSuccess"
                      :on-error="onUploadError"
                      :disabled="uploading"
                    >
                      <el-button type="primary" :loading="uploading" :icon="Upload">
                        {{ uploading ? '导入中...' : '选择并导入 XLS 文件' }}
                      </el-button>
                    </el-upload>
                    <span class="upload-hint">支持 .xls，文件大小不超过 50MB</span>
                  </div>

                  <el-divider direction="vertical" />

                  <div class="analyze-area">
                    <span class="label">输入 n（0-10）：</span>
                    <el-input-number v-model="analyzeN" :min="0" :max="10" size="small" controls-position="right" />
                    <el-button type="success" size="small" :icon="Histogram" @click="startAnalyze" :loading="analyzing">
                      开始分析
                    </el-button>
                    <span class="analyze-hint">仅分析当前查询日期 {{ searchDate }} 的数据</span>
                  </div>

                  <el-divider direction="vertical" />

                  <div class="delete-area">
                    <el-button type="danger" size="small" :icon="Delete" @click="confirmClearAll">
                      清空数据
                    </el-button>
                  </div>

                  <el-divider direction="vertical" />

                  <div class="add-area">
                    <el-button type="primary" size="small" :icon="Plus" @click="showAddDialog = true">
                      手动录入
                    </el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-dialog
            v-model="showAddDialog"
            title="手动录入数据"
            :width="dialogWidthLarge"
            :fullscreen="isMobile"
            destroy-on-close
          >
            <div class="add-dialog-content">
              <p class="dialog-tip">请输入期号和 10 个开奖号码。未带日期前缀的期号会自动补全为当前查询日期。</p>
              <div class="date-prefix-info">当前日期前缀：<strong>{{ datePrefix }}</strong></div>

              <div class="table-scroll table-scroll-wide">
              <el-table :data="manualEntryList" border style="width: 100%">
                <el-table-column label="期号" width="180">
                  <template #default="{ row }">
                    <el-input v-model="row.issueNo" placeholder="请输入期号" size="small" />
                  </template>
                </el-table-column>
                <el-table-column v-for="n in 10" :key="n" :label="`第${n}名`" width="75" align="center">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row[`ball${n}`]"
                      :min="1"
                      :max="10"
                      :controls="false"
                      placeholder="-"
                      size="small"
                      style="width: 100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ $index }">
                    <el-button type="danger" :icon="Delete" circle size="small" @click="removeManualRow($index)" />
                  </template>
                </el-table-column>
              </el-table>
              </div>

              <div class="add-row-btn">
                <el-button type="primary" plain :icon="Plus" @click="addManualRow" style="width: 100%; margin-top: 10px">
                  添加一行
                </el-button>
              </div>
            </div>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="showAddDialog = false">取消</el-button>
                <el-button type="primary" @click="handleBatchAdd" :loading="batchAdding">提交录入</el-button>
              </span>
            </template>
          </el-dialog>

          <el-row v-if="analyzeResult" class="analyze-row">
            <el-col :span="24">
              <el-card shadow="hover">
                <template #header>
                  <div class="card-header analyze-header">
                    <span>
                      数据分析结果（日期 {{ searchDate }}，输入 n={{ analyzeResult.inputN }}，分析第{{ analyzeResult.ballColumn }}名，
                      起始期号 {{ analyzeResult.startIssueNo }}）
                    </span>
                    <div class="header-actions">
                      <el-radio-group v-model="showPseudoHits" size="small">
                        <el-radio-button :label="true">显示伪命中</el-radio-button>
                        <el-radio-button :label="false">隐藏伪命中</el-radio-button>
                      </el-radio-group>
                      <el-button text @click="analyzeResult = null">关闭</el-button>
                    </div>
                  </div>
                </template>

                <div class="table-scroll">
                <el-table :data="displayedAnalyzeRules" border stripe style="width: 100%">
                  <el-table-column label="规则序号" width="120" align="center">
                    <template #default="{ row }">规则 {{ row.ruleNo }}</template>
                  </el-table-column>
                  <el-table-column label="规则内容" min-width="260">
                    <template #default="{ row }">{{ getRuleContent(row.ruleNo) }}</template>
                  </el-table-column>
                  <el-table-column label="连续超过6次未命中总次数" width="220" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.totalMissCount > 0 ? 'danger' : 'info'">{{ row.totalMissCount }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="起始期号列表" min-width="320">
                    <template #default="{ row }">
                      <div v-if="row.missEvents.length" class="issue-list">
                        <template v-for="event in row.missEvents" :key="`${row.ruleNo}-${event.displayIssueNo}-${event.actualIssueNo}`">
                          <el-tooltip
                            v-if="event.pseudoHit"
                            :content="`实际起始期号：${event.actualIssueNo}`"
                            placement="top"
                          >
                            <el-tag size="small" class="issue-tag pseudo-issue-tag">{{ event.displayIssueNo }}</el-tag>
                          </el-tooltip>
                          <el-tag v-else size="small" class="issue-tag">{{ event.displayIssueNo }}</el-tag>
                        </template>
                      </div>
                      <span v-else class="empty-text">无</span>
                    </template>
                  </el-table-column>
                </el-table>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row class="table-row">
            <el-col :span="24">
              <el-card shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>数据记录（{{ searchDate }}）</span>
                    <el-button size="small" :icon="Refresh" @click="loadData">刷新</el-button>
                  </div>
                </template>

                <div class="table-scroll table-scroll-wide">
                <el-table :data="tableData" border stripe v-loading="tableLoading" style="width: 100%">
                  <el-table-column prop="issueNo" label="期数" width="150" align="center" fixed="left" />
                  <el-table-column v-for="n in 10" :key="n" :prop="`ball${n}`" :label="`第${n}名`" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getBallTagType(row[`ball${n}`])" effect="dark" size="small">{{ row[`ball${n}`] }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="导入时间" width="180" align="center">
                    <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="120" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" size="small" link @click="editRecord(row)">修改</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                </div>

                <div class="pagination-area">
                  <el-pagination
                    v-model:current-page="pagination.page"
                    v-model:page-size="pagination.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="pagination.total"
                    :layout="paginationLayout"
                    :small="isMobile"
                    background
                    @size-change="onSizeChange"
                    @current-change="onPageChange"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-dialog
            v-model="showEditDialog"
            title="修改数据"
            :width="dialogWidthMedium"
            :fullscreen="isMobile"
            destroy-on-close
          >
            <div v-if="editingRow" class="edit-dialog-content">
              <el-form :model="editingRow" label-width="80px">
                <el-row :gutter="10">
                  <el-col :span="24">
                    <el-form-item label="期号">
                      <el-input v-model="editingRow.issueNo" disabled />
                    </el-form-item>
                  </el-col>
                  <el-col v-for="n in 10" :key="n" :span="4">
                    <el-form-item :label="`第${n}名`" label-width="50px">
                      <el-input-number
                        v-model="editingRow[`ball${n}`]"
                        :min="1"
                        :max="10"
                        :controls="false"
                        size="small"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </div>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="showEditDialog = false">取消</el-button>
                <el-button type="primary" @click="handleUpdateRecord" :loading="updating">提交修改</el-button>
              </span>
            </template>
          </el-dialog>
        </section>

        <section v-else-if="activeTab === 'dynamic'">
          <div class="page-header">
            <div>
              <h2 class="page-title">变动规则分析</h2>
              <p class="page-subtitle">导入规则结果集合，按与固有规则分析一致的逻辑计算未命中次数和未命中期号。</p>
            </div>
          </div>

          <el-row class="toolbar-row" :gutter="16">
            <el-col :span="24">
              <el-card shadow="hover">
                <div class="toolbar-content">
                  <div class="filter-area">
                    <span class="label">查询日期：</span>
                    <el-date-picker
                      v-model="searchDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="请选择日期"
                      :clearable="false"
                      size="small"
                    />
                    <span class="label">输入 n（0-10）：</span>
                    <el-input-number v-model="dynamicAnalyzeN" :min="0" :max="10" size="small" controls-position="right" />
                    <span class="label">规则条件：</span>
                    <el-input
                      v-model.trim="dynamicRuleKeyword"
                      placeholder="输入规则关键字"
                      size="small"
                      clearable
                      style="width: 220px"
                      @keyup.enter="handleDynamicRuleSearch"
                    />
                    <el-button size="small" type="primary" @click="handleDynamicRuleSearch">查询</el-button>
                    <el-button size="small" @click="resetDynamicRuleSearch">重置</el-button>
                    <el-button size="small" @click="resetToToday">今天</el-button>
                  </div>

                  <el-divider direction="vertical" />

                  <div class="upload-area">
                    <el-upload
                      :action="dynamicImportUrl"
                      name="file"
                      accept=".xls"
                      :show-file-list="false"
                      :before-upload="beforeDynamicUpload"
                      :on-success="onDynamicUploadSuccess"
                      :on-error="onDynamicUploadError"
                      :disabled="dynamicUploading"
                    >
                      <el-button type="primary" :loading="dynamicUploading" :icon="Upload">
                        {{ dynamicUploading ? '导入中...' : '导入规则 XLS 文件' }}
                      </el-button>
                    </el-upload>
                    <span class="upload-hint">参考文件：规则结果集合.xls</span>
                  </div>

                  <el-divider direction="vertical" />

                  <div class="analyze-area">
                    <el-button type="success" size="small" :icon="Histogram" :loading="dynamicAnalyzing" @click="analyzeDynamicRules">
                      分析
                    </el-button>
                    <el-button type="primary" size="small" :loading="dynamicSaving" :disabled="!dynamicAnalyzeSummary" @click="saveDynamicAnalysis">
                      保存
                    </el-button>
                    <el-button type="warning" size="small" :loading="dynamicBatchSaving" @click="saveAllDynamicAnalysis">
                      一键保存
                    </el-button>
                    <span class="analyze-hint">分析逻辑与固有规则分析一致，仅规则来源改为导入文件</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row v-if="dynamicAnalyzeSummary" class="analyze-row">
            <el-col :span="24">
              <el-card shadow="hover">
                <div class="summary-text">
                  分析完成：日期 {{ searchDate }}，输入 n={{ dynamicAnalyzeSummary.inputN }}，分析第{{ dynamicAnalyzeSummary.ballColumn }}名，
                  起始期号 {{ dynamicAnalyzeSummary.startIssueNo }}
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="24">
              <el-card shadow="hover" class="placeholder-card">
                <template #header>
                  <div class="card-header">
                    <div class="header-actions">
                      <el-radio-group v-model="showDynamicPseudoHits" size="small">
                        <el-radio-button :label="true">显示伪命中</el-radio-button>
                        <el-radio-button :label="false">隐藏伪命中</el-radio-button>
                      </el-radio-group>
                    </div>
                    <span>导入规则列表</span>
                  </div>
                </template>

                <div class="table-scroll">
                <el-table :data="displayedDynamicRules" border stripe style="width: 100%" @sort-change="handleDynamicSortChange">
                  <el-table-column prop="ruleNo" label="序号" width="100" align="center" />
                  <el-table-column prop="importedRule" label="导入规则" min-width="220" />
                  <el-table-column prop="totalMissCount" label="连续超过6次未命中的次数" width="220" align="center" sortable="custom">
                    <template #default="{ row }">
                      <el-tag :type="row.totalMissCount > 0 ? 'danger' : 'info'">{{ row.totalMissCount ?? 0 }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="未命中期号" min-width="320">
                    <template #default="{ row }">
                      <div v-if="row.missEvents && row.missEvents.length" class="issue-list">
                        <template v-for="event in row.missEvents" :key="`${row.ruleNo}-${event.displayIssueNo}-${event.actualIssueNo}`">
                          <el-tooltip
                            v-if="event.pseudoHit"
                            :content="`实际起始期号：${event.actualIssueNo}`"
                            placement="top"
                          >
                            <el-tag size="small" class="issue-tag pseudo-issue-tag">{{ event.displayIssueNo }}</el-tag>
                          </el-tooltip>
                          <el-tag v-else size="small" class="issue-tag">{{ event.displayIssueNo }}</el-tag>
                        </template>
                      </div>
                      <span v-else class="empty-text">无</span>
                    </template>
                  </el-table-column>
                </el-table>
                </div>

                <el-empty v-if="!dynamicRules.length" description="请先导入规则文件" />
              </el-card>
            </el-col>
          </el-row>
        </section>

        <section v-else-if="activeTab === 'history'">
          <div class="page-header">
            <div>
              <h2 class="page-title">变动规则历史分析</h2>
              <p class="page-subtitle">列表数据来源于变动分析记录表，支持按日期、规则、名次查询及分页。</p>
            </div>
          </div>

          <el-row class="toolbar-row" :gutter="16">
            <el-col :span="24">
              <el-card shadow="hover">
                <div class="toolbar-content">
                  <div class="filter-area">
                    <span class="label">日期：</span>
                    <el-date-picker
                      v-model="historyFilters.sourceDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="请选择日期"
                      clearable
                      size="small"
                    />
                    <span class="label">期号：</span>
                    <el-input
                      v-model.trim="historyFilters.issueNo"
                      placeholder="输入期号"
                      clearable
                      size="small"
                      style="width: 180px"
                      @keyup.enter="loadHistoryRecords(true)"
                    />
                    <span class="label">规则：</span>
                    <el-input
                      v-model.trim="historyFilters.dynamicRule"
                      placeholder="输入变动规则"
                      clearable
                      size="small"
                      style="width: 220px"
                      @keyup.enter="loadHistoryRecords(true)"
                    />
                    <span class="label">名次：</span>
                    <el-select v-model="historyFilters.rankNo" clearable size="small" placeholder="选择名次" style="width: 140px">
                      <el-option v-for="n in 10" :key="n" :label="`第${n}名`" :value="n" />
                    </el-select>
                    <el-button size="small" type="primary" @click="loadHistoryRecords(true)">查询</el-button>
                    <el-button size="small" @click="resetHistoryFilters">无条件查询</el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row class="table-row">
            <el-col :span="24">
              <el-card shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>变动分析历史记录</span>
                    <el-button size="small" :icon="Refresh" @click="loadHistoryRecords()">刷新</el-button>
                  </div>
                </template>

                <div class="table-scroll">
                <div class="table-toolbar">
                  <el-radio-group v-model="showHistoryPseudoHits" size="small">
                    <el-radio-button :label="true">显示伪命中</el-radio-button>
                    <el-radio-button :label="false">隐藏伪命中</el-radio-button>
                  </el-radio-group>
                </div>
                <el-table :data="displayedHistoryTableData" border stripe v-loading="historyLoading" style="width: 100%" @sort-change="handleHistorySortChange">
                  <el-table-column prop="sourceDate" label="数据来源日期" width="140" align="center" />
                  <el-table-column prop="rankNo" label="名次" width="100" align="center">
                    <template #default="{ row }">第{{ row.rankNo }}名</template>
                  </el-table-column>
                  <el-table-column prop="dynamicRule" label="变动规则" min-width="220" />
                  <el-table-column prop="totalMissCount" label="连续超过6次未命中的次数" width="220" align="center" sortable="custom" />
                  <el-table-column label="期号" min-width="320">
                    <template #default="{ row }">
                      <div v-if="row.displayedIssuePairs.length" class="issue-list">
                        <template v-for="pair in row.displayedIssuePairs" :key="`${row.id}-${pair.displayIssueNo}-${pair.actualIssueNo}`">
                          <el-tooltip
                            v-if="pair.pseudoHit"
                            :content="`实际起始期号：${pair.actualIssueNo}`"
                            placement="top"
                          >
                            <el-tag size="small" class="issue-tag pseudo-issue-tag">{{ pair.displayIssueNo }}</el-tag>
                          </el-tooltip>
                          <el-tag v-else size="small" class="issue-tag">{{ pair.displayIssueNo }}</el-tag>
                        </template>
                      </div>
                      <span v-else class="empty-text">无</span>
                    </template>
                  </el-table-column>
                </el-table>
                </div>

                <div class="pagination-area">
                  <el-pagination
                    v-model:current-page="historyPagination.page"
                    v-model:page-size="historyPagination.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="historyPagination.total"
                    :layout="paginationLayout"
                    :small="isMobile"
                    background
                    @size-change="onHistorySizeChange"
                    @current-change="onHistoryPageChange"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </section>

        <section v-else>
          <div class="page-header">
            <div>
              <h2 class="page-title">多日期比较分析</h2>
              <p class="page-subtitle">只选择日期和名次，系统会自动对选中日期下相同名次、相同规则的记录进行比较。</p>
            </div>
          </div>

          <el-row class="toolbar-row" :gutter="16">
            <el-col :span="24">
              <el-card shadow="hover">
<div class="toolbar-content compare-toolbar">
                  <div class="filter-area">
                    <span class="label">日期：</span>
                    <el-date-picker
                      v-model="compareFilters.sourceDates"
                      type="dates"
                      value-format="YYYY-MM-DD"
                      placeholder="手动选择多个日期"
                      size="small"
                    />
                    <span class="label">名次：</span>
                    <el-select v-model="compareFilters.rankNo" size="small" placeholder="选择名次" style="width: 140px">
                      <el-option v-for="n in 10" :key="n" :label="`第${n}名`" :value="n" />
                    </el-select>
                    <span class="label">规则：</span>
                    <el-input
                      v-model.trim="compareFilters.dynamicRule"
                      placeholder="输入规则关键字"
                      clearable
                      size="small"
                      style="width: 220px"
                      @keyup.enter="compareHistoryRecords"
                    />
                    <el-button type="primary" size="small" :loading="compareLoading" @click="compareHistoryRecords">比较分析</el-button>
                    <el-button size="small" @click="resetCompareFilters">重置</el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row v-if="historyCompareResult" class="table-row">
            <el-col :span="24">
              <el-card shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>比较结果</span>
                    <el-radio-group :model-value="compareSortOrder" size="small" @change="handleCompareSortChange">
                      <el-radio-button label="">默认</el-radio-button>
                      <el-radio-button label="descending">汇总降序</el-radio-button>
                      <el-radio-button label="ascending">汇总升序</el-radio-button>
                    </el-radio-group>
                  </div>
                </template>

                <el-row class="compare-summary-row" :gutter="16">
                  <el-col :span="8">
                    <el-statistic title="名次" :value="historyCompareResult.rankNo">
                      <template #formatter>{{ `第${historyCompareResult.rankNo}名` }}</template>
                    </el-statistic>
                  </el-col>
                  <el-col :span="8">
                    <el-statistic title="比较日期数" :value="compareFilters.sourceDates.length" />
                  </el-col>
                  <el-col :span="8">
                    <el-statistic title="规则数" :value="historyCompareResult.ruleCompares?.length || 0" />
                  </el-col>
                </el-row>

                <el-collapse class="compare-collapse">
                  <div class="table-toolbar">
                    <el-radio-group v-model="showComparePseudoHits" size="small">
                      <el-radio-button :label="true">显示伪命中</el-radio-button>
                      <el-radio-button :label="false">隐藏伪命中</el-radio-button>
                    </el-radio-group>
                  </div>
                  <el-collapse-item
                    v-for="ruleCompare in displayedRuleCompares"
                    :key="ruleCompare.dynamicRule"
                    :title="`${ruleCompare.dynamicRule}｜次数汇总：${ruleCompare.totalMissCountSum || 0}`"
                    :name="ruleCompare.dynamicRule"
                  >
                    <div class="table-scroll">
                    <el-table :data="ruleCompare.items || []" border stripe style="width: 100%">
                      <el-table-column prop="sourceDate" label="日期" width="140" align="center" />
                      <el-table-column prop="totalMissCount" label="连续超过6次未命中的次数" width="220" align="center" />
                      <el-table-column label="期号" min-width="320">
                        <template #default="{ row }">
                          <div v-if="row.displayedIssuePairs.length" class="issue-list">
                            <template
                              v-for="pair in row.displayedIssuePairs"
                              :key="`${ruleCompare.dynamicRule}-${row.sourceDate}-${pair.displayIssueNo}-${pair.actualIssueNo}`"
                            >
                              <el-tooltip
                                v-if="pair.pseudoHit"
                                :content="`实际起始期号：${pair.actualIssueNo}`"
                                placement="top"
                              >
                                <el-tag size="small" class="issue-tag pseudo-issue-tag">{{ pair.displayIssueNo }}</el-tag>
                              </el-tooltip>
                              <el-tag v-else size="small" class="issue-tag">{{ pair.displayIssueNo }}</el-tag>
                            </template>
                          </div>
                          <span v-else class="empty-text">无</span>
                        </template>
                      </el-table-column>
                    </el-table>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </el-card>
            </el-col>
          </el-row>

          <el-empty v-else description="请选择多个日期和名次后进行比较分析" />
        </section>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Delete, Histogram, Operation, Plus, Refresh, Tickets, TrendCharts, Upload } from '@element-plus/icons-vue'

const formatCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 页面主视图状态，控制四类分析场景切换。
const activeTab = ref('inherent')
const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 768 : false)
const searchDate = ref(formatCurrentDate())
const datePrefix = computed(() => searchDate.value.replaceAll('-', ''))
const issueSearch = ref('')
const paginationLayout = computed(() => (isMobile.value ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'))
const dialogWidthLarge = computed(() => (isMobile.value ? '100%' : '1000px'))
const dialogWidthMedium = computed(() => (isMobile.value ? '100%' : '900px'))

// 手工录入与编辑弹窗状态。
const showAddDialog = ref(false)
const manualEntryList = ref([])
const batchAdding = ref(false)

const showEditDialog = ref(false)
const editingRow = ref(null)
const updating = ref(false)

const importUrl = '/api/lottery/import'
const dynamicImportUrl = '/api/lottery/dynamic-rules/import'
const uploading = ref(false)
const dynamicUploading = ref(false)
const tableLoading = ref(false)
const tableData = ref([])
const pagination = ref({ page: 1, size: 20, total: 0 })

// 固定规则分析区状态。
const analyzeN = ref(1)
const analyzing = ref(false)
const analyzeResult = ref(null)
const showPseudoHits = ref(true)

// 动态规则分析与保存状态。
const dynamicAnalyzeN = ref(1)
const dynamicAnalyzing = ref(false)
const dynamicSaving = ref(false)
const dynamicBatchSaving = ref(false)
const dynamicRules = ref([])
const dynamicAnalyzeSummary = ref(null)
const dynamicSortOrder = ref('')
const dynamicRuleKeyword = ref('')
const showDynamicPseudoHits = ref(true)

const historyFilters = ref({
  sourceDate: '',
  issueNo: '',
  dynamicRule: '',
  rankNo: null,
})
const historyTableData = ref([])
const historyLoading = ref(false)
const historyPagination = ref({ page: 1, size: 20, total: 0 })
const historySortOrder = ref('')
const showHistoryPseudoHits = ref(true)

const compareFilters = ref({
  sourceDates: [],
  rankNo: null,
  dynamicRule: '',
})
const compareLoading = ref(false)
const historyCompareResult = ref(null)
const compareSortOrder = ref('')
const showComparePseudoHits = ref(true)

const ruleContentMap = {
  1: '小大大小小小大大小小',
  2: '小大大小大大小大小小',
  3: '大小小大大大小小大大',
  4: '大小小大小小大小小大',
}

const displayedAnalyzeRules = computed(() => {
  if (!analyzeResult.value) {
    return []
  }

  return analyzeResult.value.rules.map((rule) => {
    const missEvents = filterPseudoMissEvents(rule.missEvents, showPseudoHits.value)

    return {
      ...rule,
      totalMissCount: missEvents.length,
      missEvents,
    }
  })
})

const updatedRuleContentMap = {
  1: '小大大小小小大小大大',
  2: '小大大小大大小小大大',
  3: '大小小大大大小大小小',
  4: '大小小大小小大大小小',
}

const displayedDynamicRules = computed(() => {
  const keyword = dynamicRuleKeyword.value.trim()
  const rules = dynamicRules.value
    .filter((rule) => !keyword || (rule.importedRule || '').includes(keyword))
    .map((rule) => {
      const missEvents = filterPseudoMissEvents(rule.missEvents, showDynamicPseudoHits.value)

      return {
        ...rule,
        totalMissCount: missEvents.length,
        missEvents,
      }
    })

  if (dynamicSortOrder.value === 'ascending') {
    rules.sort((a, b) => (a.totalMissCount ?? 0) - (b.totalMissCount ?? 0))
  } else if (dynamicSortOrder.value === 'descending') {
    rules.sort((a, b) => (b.totalMissCount ?? 0) - (a.totalMissCount ?? 0))
  }

  return rules
})

const displayedHistoryTableData = computed(() =>
  historyTableData.value.map((row) => {
    const displayedIssuePairs = filterIssuePairs(row.issueNos, row.actualIssueNos, showHistoryPseudoHits.value)

    return {
      ...row,
      totalMissCount: displayedIssuePairs.length,
      displayedIssuePairs,
    }
  })
)

const displayedRuleCompares = computed(() => {
  const ruleCompares = (historyCompareResult.value?.ruleCompares || []).map((ruleCompare) => {
    const items = (ruleCompare.items || []).map((item) => {
      const displayedIssuePairs = filterIssuePairs(item.issueNos, item.actualIssueNos, showComparePseudoHits.value)

      return {
        ...item,
        totalMissCount: displayedIssuePairs.length,
        displayedIssuePairs,
      }
    })

    return {
      ...ruleCompare,
      totalMissCountSum: items.reduce((sum, item) => sum + (item.totalMissCount ?? 0), 0),
      items,
    }
  })

  if (compareSortOrder.value === 'ascending') {
    ruleCompares.sort((a, b) => (a.totalMissCountSum ?? 0) - (b.totalMissCountSum ?? 0))
  } else if (compareSortOrder.value === 'descending') {
    ruleCompares.sort((a, b) => (b.totalMissCountSum ?? 0) - (a.totalMissCountSum ?? 0))
  }

  return ruleCompares
})

const handleTabChange = (tab) => {
  activeTab.value = tab
  if (tab === 'dynamic') {
    loadDynamicRules()
  } else if (tab === 'history') {
    loadHistoryRecords(true)
  }
}

const handleDynamicSortChange = ({ prop, order }) => {
  if (prop === 'totalMissCount') {
    dynamicSortOrder.value = order || ''
  }
}

const handleDynamicRuleSearch = () => {
  dynamicSortOrder.value = ''
}

const resetDynamicRuleSearch = () => {
  dynamicRuleKeyword.value = ''
  dynamicSortOrder.value = ''
}

const handleHistorySortChange = ({ prop, order }) => {
  if (prop === 'totalMissCount') {
    historySortOrder.value = order || ''
    loadHistoryRecords(true)
  }
}

const resetHistoryFilters = () => {
  historyFilters.value = { sourceDate: '', issueNo: '', dynamicRule: '', rankNo: null }
  historySortOrder.value = ''
  loadHistoryRecords(true)
}

const resetCompareFilters = () => {
  compareFilters.value = { sourceDates: [], rankNo: null, dynamicRule: '' }
  historyCompareResult.value = null
  compareSortOrder.value = ''
}

const handleCompareSortChange = (order) => {
  compareSortOrder.value = order
}

const addManualRow = () => {
  const row = { issueNo: '' }
  for (let i = 1; i <= 10; i++) {
    row[`ball${i}`] = undefined
  }
  manualEntryList.value.push(row)
}

const removeManualRow = (index) => {
  manualEntryList.value.splice(index, 1)
}

watch(showAddDialog, (visible) => {
  if (visible && manualEntryList.value.length === 0) {
    addManualRow()
  }
  if (!visible) {
    manualEntryList.value = []
  }
})

const getRuleContent = (ruleNo) => updatedRuleContentMap[ruleNo] || ruleContentMap[ruleNo] || '-'

const filterPseudoMissEvents = (missEvents, showPseudo) => {
  const sourceEvents = missEvents || []
  return showPseudo ? sourceEvents : sourceEvents.filter((event) => !event.pseudoHit)
}

// 触发固定规则分析，并同步刷新分析面板。
const startAnalyze = async () => {
  analyzing.value = true
  try {
    const resp = await axios.get('/api/lottery/analyze', {
      params: { n: analyzeN.value, queryDate: searchDate.value },
    })

    if (resp.data.code === 200) {
      analyzeResult.value = resp.data.data
      showPseudoHits.value = true
      if (!analyzeResult.value) {
        ElMessage.warning(`未找到 ${searchDate.value} 的符合条件数据`)
      }
    } else {
      ElMessage.error(resp.data.message || '分析失败')
    }
  } catch {
    ElMessage.error('分析失败')
  } finally {
    analyzing.value = false
  }
}

const confirmClearAll = () => {
  ElMessageBox.confirm('确定要清空数据库中所有的数据记录吗？此操作不可恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        const resp = await axios.delete('/api/lottery/clear')
        if (resp.data.code === 200) {
          ElMessage.success('数据已清空')
          analyzeResult.value = null
          dynamicAnalyzeSummary.value = null
          loadData(true)
        } else {
          ElMessage.error(resp.data.message || '清空失败')
        }
      } catch {
        ElMessage.error('清空失败')
      }
    })
    .catch(() => {})
}

const handleBatchAdd = async () => {
  const sourceRecords = manualEntryList.value.filter((row) => row.issueNo && row.issueNo.trim())
  if (sourceRecords.length === 0) {
    ElMessage.warning('请输入有效的期号和数据')
    return
  }

  const records = sourceRecords.map((row) => {
    const record = { ...row }
    if (!record.issueNo.startsWith(datePrefix.value)) {
      record.issueNo = datePrefix.value + record.issueNo
    }
    return record
  })

  for (let i = 0; i < records.length; i++) {
    for (let j = 1; j <= 10; j++) {
      if (records[i][`ball${j}`] === undefined || records[i][`ball${j}`] === null) {
        ElMessage.error(`第 ${i + 1} 行数据不完整，第 ${j} 名号码未输入`)
        return
      }
    }
  }

  batchAdding.value = true
  try {
    const resp = await axios.post('/api/lottery/batch-add', records)
    if (resp.data.code === 200) {
      ElMessage.success(`录入成功：新增 ${resp.data.data.insertedCount} 条，更新 ${resp.data.data.updatedCount} 条`)
      showAddDialog.value = false
      loadData(true)
    } else {
      ElMessage.error(resp.data.message || '录入失败')
    }
  } catch {
    ElMessage.error('网络错误或服务器异常')
  } finally {
    batchAdding.value = false
  }
}

const editRecord = (row) => {
  editingRow.value = JSON.parse(JSON.stringify(row))
  showEditDialog.value = true
}

const handleUpdateRecord = async () => {
  if (!editingRow.value) {
    return
  }
  updating.value = true
  try {
    const resp = await axios.post('/api/lottery/batch-add', [editingRow.value])
    if (resp.data.code === 200) {
      ElMessage.success('修改成功')
      showEditDialog.value = false
      loadData()
    } else {
      ElMessage.error(resp.data.message || '修改失败')
    }
  } catch {
    ElMessage.error('修改失败')
  } finally {
    updating.value = false
  }
}

const validateXlsFile = (file, loadingRef) => {
  const isXls = file.name.toLowerCase().endsWith('.xls')
  if (!isXls) {
    ElMessage.error('只能上传 .xls 格式文件')
    return false
  }
  loadingRef.value = true
  return true
}

const beforeUpload = (file) => validateXlsFile(file, uploading)
const beforeDynamicUpload = (file) => validateXlsFile(file, dynamicUploading)

const onUploadSuccess = (response) => {
  uploading.value = false
  if (response && response.code === 200) {
    ElMessage.success(`导入成功：新增 ${response.data.insertedCount} 条，更新 ${response.data.updatedCount} 条`)
    loadData(true)
  } else {
    ElMessage.error(`导入失败：${response?.message || '未知错误'}`)
  }
}

const onUploadError = () => {
  uploading.value = false
  ElMessage.error('上传失败')
}

const onDynamicUploadSuccess = (response) => {
  dynamicUploading.value = false
  if (response && response.code === 200) {
    dynamicRules.value = response.data || []
    dynamicAnalyzeSummary.value = null
    dynamicSortOrder.value = ''
    ElMessage.success(`规则导入成功，共识别 ${dynamicRules.value.length} 条规则`)
  } else {
    ElMessage.error(`规则导入失败：${response?.message || '未知错误'}`)
  }
}

const onDynamicUploadError = () => {
  dynamicUploading.value = false
  ElMessage.error('规则上传失败')
}

// 使用当前日期和名次，对已导入的动态规则执行分析。
const analyzeDynamicRules = async () => {
  if (!dynamicRules.value.length) {
    ElMessage.warning('请先导入规则文件')
    return
  }

  dynamicAnalyzing.value = true
  try {
    const resp = await axios.post('/api/lottery/dynamic-rules/analyze', {
      n: dynamicAnalyzeN.value,
      queryDate: searchDate.value,
    })

    if (resp.data.code === 200) {
      const result = resp.data.data
      if (!result) {
        dynamicAnalyzeSummary.value = null
        dynamicRules.value = dynamicRules.value.map((item) => ({ ...item, totalMissCount: 0, missEvents: [] }))
        dynamicSortOrder.value = ''
        showDynamicPseudoHits.value = true
        ElMessage.warning(`未找到 ${searchDate.value} 的符合条件数据`)
        return
      }
      dynamicAnalyzeSummary.value = result
      dynamicRules.value = result.rules || []
      dynamicSortOrder.value = ''
      showDynamicPseudoHits.value = true
      ElMessage.success('规则分析完成')
    } else {
      ElMessage.error(resp.data.message || '规则分析失败')
    }
  } catch {
    ElMessage.error('规则分析失败')
  } finally {
    dynamicAnalyzing.value = false
  }
}

const saveDynamicAnalysis = async () => {
  if (!dynamicAnalyzeSummary.value || !dynamicRules.value.length) {
    ElMessage.warning('请先完成变动规则分析')
    return
  }

  dynamicSaving.value = true
  try {
    const resp = await axios.post('/api/lottery/dynamic-rules/save', {
      queryDate: searchDate.value,
      rankNo: dynamicAnalyzeSummary.value.ballColumn,
      rules: dynamicRules.value,
    })
    if (resp.data.code === 200) {
      ElMessage.success('变动分析结果已保存')
      loadHistoryRecords(true)
    } else {
      ElMessage.error(resp.data.message || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    dynamicSaving.value = false
  }
}

const saveAllDynamicAnalysis = async () => {
  dynamicBatchSaving.value = true
  try {
    const resp = await axios.post('/api/lottery/dynamic-rules/save-all', {
      queryDate: searchDate.value,
    })
    if (resp.data.code === 200) {
      ElMessage.success('所选日期 1-10 名分析结果已全部保存')
      loadHistoryRecords(true)
    } else {
      ElMessage.error(resp.data.message || '一键保存失败')
    }
  } catch {
    ElMessage.error('一键保存失败')
  } finally {
    dynamicBatchSaving.value = false
  }
}

const loadDynamicRules = async () => {
  try {
    const resp = await axios.get('/api/lottery/dynamic-rules')
    if (resp.data.code === 200) {
      dynamicRules.value = (resp.data.data || []).map((item) => ({
        ruleNo: item.ruleNo,
        importedRule: item.rulePattern,
        totalMissCount: 0,
        missEvents: [],
      }))
      dynamicSortOrder.value = ''
    } else {
      ElMessage.error(resp.data.message || '规则加载失败')
    }
  } catch {
    ElMessage.error('规则加载失败')
  }
}

// 加载动态规则历史分析记录，供历史页分页展示。
const loadHistoryRecords = async (resetPage = false) => {
  if (resetPage) {
    historyPagination.value.page = 1
  }

  historyLoading.value = true
  try {
    const resp = await axios.get('/api/lottery/dynamic-analysis-records', {
      params: {
        page: historyPagination.value.page,
        size: historyPagination.value.size,
        sourceDate: historyFilters.value.sourceDate || undefined,
        issueNo: historyFilters.value.issueNo || undefined,
        dynamicRule: historyFilters.value.dynamicRule || undefined,
        rankNo: historyFilters.value.rankNo || undefined,
        sortOrder: historySortOrder.value || undefined,
      },
    })
    if (resp.data.code === 200) {
      historyTableData.value = resp.data.data.records
      historyPagination.value.total = resp.data.data.total
      historyPagination.value.page = resp.data.data.current
      historyPagination.value.size = resp.data.data.size
      showHistoryPseudoHits.value = true
    } else {
      ElMessage.error(resp.data.message || '历史记录加载失败')
    }
  } catch {
    ElMessage.error('历史记录加载失败')
  } finally {
    historyLoading.value = false
  }
}

// 对多个日期的历史分析结果做横向比较。
const compareHistoryRecords = async () => {
  if (!compareFilters.value.sourceDates || compareFilters.value.sourceDates.length < 2) {
    ElMessage.warning('请至少选择两个日期')
    return
  }
  if (!compareFilters.value.rankNo) {
    ElMessage.warning('请选择名次')
    return
  }

  compareLoading.value = true
  try {
    const resp = await axios.post('/api/lottery/dynamic-analysis-records/compare', {
      sourceDates: compareFilters.value.sourceDates,
      rankNo: compareFilters.value.rankNo,
      dynamicRule: compareFilters.value.dynamicRule || undefined,
    })
    if (resp.data.code === 200) {
      historyCompareResult.value = resp.data.data
      compareSortOrder.value = ''
      showComparePseudoHits.value = true
      if (!historyCompareResult.value?.ruleCompares?.length) {
        ElMessage.warning('未找到符合条件的比较记录')
      }
    } else {
      ElMessage.error(resp.data.message || '比较分析失败')
    }
  } catch {
    ElMessage.error('比较分析失败')
  } finally {
    compareLoading.value = false
  }
}

// 加载当前日期下的数据记录列表，同时重置固定规则分析结果。
const loadData = async (resetPage = false) => {
  if (resetPage) {
    pagination.value.page = 1
  }

  tableLoading.value = true
  try {
    const resp = await axios.get('/api/lottery/list', {
      params: {
        page: pagination.value.page,
        size: pagination.value.size,
        queryDate: searchDate.value,
        startIssueNo: issueSearch.value || undefined,
      },
    })
    if (resp.data.code === 200) {
      tableData.value = resp.data.data.records
      pagination.value.total = resp.data.data.total
      pagination.value.page = resp.data.data.current
      pagination.value.size = resp.data.data.size
      analyzeResult.value = null
    } else {
      ElMessage.error(resp.data.message || '数据加载失败')
    }
  } catch {
    ElMessage.error('数据加载失败')
  } finally {
    tableLoading.value = false
  }
}

const handleDateChange = () => {
  loadData(true)
}

const handleSearch = () => {
  loadData(true)
}

const resetToToday = () => {
  searchDate.value = formatCurrentDate()
  issueSearch.value = ''
  if (activeTab.value === 'inherent') {
    loadData(true)
  }
}

const onSizeChange = (val) => {
  pagination.value.size = val
  loadData(true)
}

const onPageChange = (val) => {
  pagination.value.page = val
  loadData()
}

const onHistorySizeChange = (val) => {
  historyPagination.value.size = val
  loadHistoryRecords(true)
}

const onHistoryPageChange = (val) => {
  historyPagination.value.page = val
  loadHistoryRecords()
}

const getBallTagType = (val) => (val <= 5 ? 'danger' : 'success')

const formatDateTime = (str) => {
  if (!str) {
    return '-'
  }
  return str.replace('T', ' ').substring(0, 19)
}

const syncViewport = () => {
  isMobile.value = window.innerWidth <= 768
}

// 将展示期号和实际期号组装成前端表格可直接消费的结构。
const buildIssuePairs = (issueNos, actualIssueNos) => {
  const displayList = (issueNos || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
  const actualList = (actualIssueNos || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)

  return displayList.map((displayIssueNo, index) => {
    const actualIssueNo = actualList[index] || displayIssueNo
    return {
      displayIssueNo,
      actualIssueNo,
      pseudoHit: displayIssueNo !== actualIssueNo,
    }
  })
}

const filterIssuePairs = (issueNos, actualIssueNos, showPseudo) => {
  const pairs = buildIssuePairs(issueNos, actualIssueNos)
  return showPseudo ? pairs : pairs.filter((pair) => !pair.pseudoHit)
}

// 页面初始化时预加载基础数据，保证四个页面首屏即可操作。
onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
  loadData(true)
  loadDynamicRules()
  loadHistoryRecords(true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #f7f9fc 0%, #eef3f9 100%);
}

.layout-container {
  min-height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #16324f 0%, #1f4d78 100%);
  color: #fff;
}

.brand-block {
  padding: 28px 22px 20px;
}

.brand-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.brand-subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
}

.nav-menu {
  border-right: none;
  background: transparent;
}

:deep(.nav-menu .el-menu-item) {
  color: rgba(255, 255, 255, 0.82);
}

:deep(.nav-menu .el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1);
}

:deep(.nav-menu .el-menu-item.is-active) {
  color: #ffffff;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
}

.main-panel {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #223046;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.toolbar-row,
.table-row {
  margin-bottom: 16px;
}

.toolbar-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-area,
.upload-area,
.analyze-area,
.add-area,
.delete-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-hint,
.analyze-hint,
.label,
.empty-text,
.summary-text {
  color: #909399;
  font-size: 13px;
}

.dialog-tip {
  margin-bottom: 8px;
  color: #606266;
}

.date-prefix-info {
  margin-bottom: 12px;
  color: #409eff;
  font-size: 14px;
}

.analyze-row {
  margin-bottom: 16px;
}

.issue-list {
  display: flex;
  flex-wrap: wrap;
}

.issue-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.pseudo-issue-tag {
  color: #fff;
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.analyze-header {
  align-items: flex-start;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.pagination-area {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.table-scroll :deep(.el-table) {
  min-width: 760px;
}

.table-scroll-wide :deep(.el-table) {
  min-width: 1120px;
}

.placeholder-card {
  min-height: 220px;
}

.compare-toolbar,
.compare-summary-row {
  margin-bottom: 16px;
}

.compare-collapse {
  margin-top: 12px;
}

@media (max-width: 960px) {
  .layout-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  }

  .main-panel {
    padding: 16px;
  }

  .brand-block {
    padding: 20px 16px 12px;
  }

  .brand-title {
    font-size: 20px;
  }

  :deep(.nav-menu.el-menu) {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
  }

  :deep(.nav-menu .el-menu-item) {
    min-width: fit-content;
    padding: 0 18px;
  }

  .toolbar-content {
    align-items: stretch;
  }

  .filter-area,
  .upload-area,
  .analyze-area,
  .add-area,
  .delete-area {
    width: 100%;
  }

  .card-header,
  .analyze-header,
  .header-actions {
    align-items: flex-start;
    justify-content: flex-start;
    flex-direction: column;
  }

  .pagination-area {
    justify-content: center;
  }

  .compare-summary-row :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}

@media (max-width: 768px) {
  .main-panel {
    padding: 12px;
  }

  .page-title {
    font-size: 22px;
  }

  .page-subtitle {
    font-size: 13px;
  }

  .toolbar-content {
    gap: 12px;
  }

  .filter-area,
  .upload-area,
  .analyze-area,
  .add-area,
  .delete-area {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .filter-area .label,
  .upload-hint,
  .analyze-hint {
    width: 100%;
  }

  .filter-area :deep(.el-date-editor),
  .filter-area :deep(.el-input),
  .filter-area :deep(.el-select),
  .filter-area :deep(.el-input-number),
  .upload-area :deep(.el-button),
  .analyze-area :deep(.el-button),
  .analyze-area :deep(.el-input-number),
  .add-area :deep(.el-button),
  .delete-area :deep(.el-button) {
    width: 100% !important;
  }

  .filter-area :deep(.el-button),
  .upload-area :deep(.el-button),
  .analyze-area :deep(.el-button),
  .add-area :deep(.el-button),
  .delete-area :deep(.el-button) {
    margin-left: 0;
  }

  .table-scroll :deep(.el-table) {
    min-width: 680px;
  }

  .table-scroll-wide :deep(.el-table) {
    min-width: 980px;
  }

  :deep(.el-dialog.is-fullscreen) {
    padding: 12px;
  }

  :deep(.el-dialog__body) {
    padding: 12px 0 0;
  }

  :deep(.el-dialog__footer) {
    padding: 12px 0 0;
  }

  .compare-collapse :deep(.el-collapse-item__header) {
    line-height: 1.5;
    height: auto;
    padding: 10px 0;
  }
}
</style>
