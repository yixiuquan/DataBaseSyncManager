// 任务监控组件
const TaskMonitor = {
    props: {
        id: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            task: null,
            taskInfo: {},
            tableStats: [],
            totalExceptionCount: 0,
            loading: true,
            timer: null,
            databases: {},
            currentError: null,
            errorDialogVisible: false,
            syncExceptions: [] // 添加同步异常数据存储
        };
    },
    created() {
        this.$emit('update-active-index', '/tasks');
        this.fetchDatabases();
        this.fetchData();
        
        // 设置定时器，每10秒刷新一次数据
        this.timer = setInterval(() => {
            this.fetchData(false);
        }, 10000);
    },
    beforeDestroy() {
        // 组件销毁前清除定时器
        if (this.timer) {
            clearInterval(this.timer);
        }
    },
    methods: {
        fetchDatabases() {
            api.database.getAllDatabases().then(res => {
                const databases = res.data || [];
                this.databases = {};
                databases.forEach(db => {
                    this.databases[db.id] = db;
                });
            });
        },
        fetchData(showLoading = true) {
            if (showLoading) {
                this.loading = true;
            }
            
            Promise.all([
                api.syncTask.getSyncTaskById(this.id),
                api.syncTask.getTaskStatistics(this.id)
            ]).then(([taskRes, statsRes]) => {
                this.task = taskRes.data;
                
                const statsData = statsRes.data || {};
                this.taskInfo = statsData.taskInfo || {};
                this.tableStats = statsData.tableStats || [];
                this.totalExceptionCount = statsData.totalExceptionCount || 0;
                
                // 如果后端状态与前端状态不一致，使用后端返回的isRunning状态更新task状态
                if (this.taskInfo.isRunning !== undefined && ((this.taskInfo.isRunning && this.task.status !== 1) || (!this.taskInfo.isRunning && this.task.status === 1))) {
                    console.log(`任务状态不一致，更新状态: 数据库状态=${this.task.status}, 实际运行状态=${this.taskInfo.isRunning}`);
                    this.task.status = this.taskInfo.isRunning ? 1 : 0;
                }
                
                // 将表统计中的进度格式化为数字，确保进度条正确显示
                this.tableStats.forEach(table => {
                    if (typeof table.progress === 'string') {
                        table.progress = parseFloat(table.progress);
                    }
                    // 确保同步计数为数字
                    if (typeof table.syncCount === 'string') {
                        table.syncCount = parseInt(table.syncCount);
                    }
                    // 确保异常计数为数字
                    if (typeof table.exceptionCount === 'string') {
                        table.exceptionCount = parseInt(table.exceptionCount);
                    }
                });
                
                this.loading = false;
            }).catch(() => {
                this.loading = false;
                this.$router.push('/tasks');
            });
        },
        formatStatus(status) {
            if (status === 0) return '已停止';
            if (status === 1) return '运行中';
            if (status === 2) return '异常';
            return '未知';
        },
        getStatusClass(status) {
            if (status === 0) return 'task-status-stopped';
            if (status === 1) return 'task-status-running';
            if (status === 2) return 'task-status-error';
            return '';
        },
        getDatabaseName(id) {
            return this.databases[id] ? this.databases[id].host + ':' + this.databases[id].port + '/' + this.databases[id].dbName : '-';
        },
        formatSyncType(type) {
            return type === 0 ? '全量同步' : '增量同步';
        },
        formatDate(date) {
            if (!date) return '-';
            return new Date(date).toLocaleString();
        },
        formatProgress(progress) {
            if (progress === -1) {
                return '持续进行中';
            }
            return `${progress.toFixed(2)}%`;
        },
        handleStart() {
            this.$confirm('确认启动该同步任务？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'info'
            }).then(() => {
                this.loading = true;
                api.syncTask.startTask(this.id).then(() => {
                    this.$message.success('任务启动成功');
                    this.fetchData();
                }).catch(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        handleStop() {
            this.$confirm('确认停止该同步任务？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.loading = true;
                api.syncTask.stopTask(this.id).then(() => {
                    this.$message.success('任务停止成功');
                    this.fetchData();
                }).catch(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        viewErrors(tableName) {
            // 使用真实数据替换模拟数据
            this.loading = true;
            
            // 调用API获取指定表的异常记录
            api.syncException.getByTaskIdAndTableName(this.id, tableName).then(res => {
                const errors = res.data || [];
                this.currentError = {
                    tableName,
                    errors: errors.map(err => ({
                        id: err.id,
                        errorDetail: err.errorMessage,
                        errorTime: err.errorTime
                    }))
                };
                this.errorDialogVisible = true;
                this.loading = false;
            }).catch(() => {
                this.loading = false;
                this.$message.error('获取异常记录失败');
            });
        }
    },
    template: `
        <div>
            <el-card shadow="hover" class="custom-card" v-loading="loading">
                <div slot="header" class="card-header">
                    <span>任务监控: {{ task ? task.taskName : '' }}</span>
                    <div>
                        <el-button size="small" type="primary" icon="el-icon-refresh" @click="fetchData">刷新</el-button>
                        <el-button size="small" type="success" @click="handleStart" :disabled="task && task.status === 1">启动</el-button>
                        <el-button size="small" type="warning" @click="handleStop" :disabled="task && task.status !== 1">停止</el-button>
                        <el-button size="small" @click="$router.push('/tasks')">返回</el-button>
                    </div>
                </div>
                
                <div v-if="task">
                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-card shadow="hover" class="monitor-card">
                                <div class="monitor-title">任务基本信息</div>
                                <el-descriptions border :column="1">
                                    <el-descriptions-item label="任务状态">
                                        <span :class="['task-status', getStatusClass(task.status)]">
                                            {{ formatStatus(task.status) }}
                                        </span>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="同步类型">{{ formatSyncType(task.syncType) }}</el-descriptions-item>
                                    <el-descriptions-item label="源数据库">{{ getDatabaseName(task.sourceDbId) }}</el-descriptions-item>
                                    <el-descriptions-item label="目标数据库">{{ getDatabaseName(task.targetDbId) }}</el-descriptions-item>
                                    <el-descriptions-item label="创建时间">{{ formatDate(task.createTime) }}</el-descriptions-item>
                                    <el-descriptions-item label="启动时间">{{ formatDate(task.taskStartTime) }}</el-descriptions-item>
                                </el-descriptions>
                            </el-card>
                        </el-col>
                        <el-col :span="12">
                            <el-card shadow="hover" class="monitor-card">
                                <div class="monitor-title">同步统计</div>
                                <div class="monitor-stats">
                                    <div class="stat-card">
                                        <div class="stat-value">{{ tableStats.length }}</div>
                                        <div class="stat-label">同步表数量</div>
                                    </div>
                                    <div class="stat-card">
                                        <div class="stat-value">{{ totalExceptionCount }}</div>
                                        <div class="stat-label">异常记录数</div>
                                    </div>
                                    <div class="stat-card">
                                        <div class="stat-value">{{ taskInfo.totalSyncCount || 0 }}</div>
                                        <div class="stat-label">已同步数据量</div>
                                    </div>
                                    <div class="stat-card">
                                        <div class="stat-value">{{ task.status === 1 ? '运行中' : (task.status === 0 ? '已停止' : '异常') }}</div>
                                        <div class="stat-label">当前状态</div>
                                    </div>
                                </div>
                            </el-card>
                        </el-col>
                    </el-row>
                    
                    <el-card shadow="hover" class="custom-card" style="margin-top: 20px;">
                        <div slot="header" class="card-header">
                            <span>表同步详情</span>
                        </div>
                        
                        <el-table :data="tableStats" border style="width: 100%">
                            <el-table-column type="index" width="50"></el-table-column>
                            <el-table-column prop="tableName" label="表名" min-width="150"></el-table-column>
                            <el-table-column label="开始时间" width="180">
                                <template slot-scope="scope">
                                    {{ formatDate(scope.row.startTime) }}
                                </template>
                            </el-table-column>
                            <el-table-column label="同步数据" width="180">
                                <template slot-scope="scope">
                                    <div>已插入: {{ scope.row.insertCount || 0 }}</div>
                                    <div>已更新: {{ scope.row.updateCount || 0 }}</div>
                                    <div>已删除: {{ scope.row.deleteCount || 0 }}</div>
                                </template>
                            </el-table-column>
                            <el-table-column label="异常数据" width="100">
                                <template slot-scope="scope">
                                    <el-button 
                                        v-if="scope.row.exceptionCount > 0" 
                                        type="text" 
                                        @click="viewErrors(scope.row.tableName)">
                                        {{ scope.row.exceptionCount }}
                                    </el-button>
                                    <span v-else>0</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="同步进度" min-width="250">
                                <template slot-scope="scope">
                                    <el-progress 
                                        :percentage="scope.row.progress === -1 ? 99 : scope.row.progress" 
                                        :status="scope.row.progress === -1 ? 'success' : null"
                                        :stroke-width="15">
                                    </el-progress>
                                    <div class="progress-info">
                                        <span class="progress-label">{{ formatProgress(scope.row.progress) }}</span>
                                    </div>
                                </template>
                            </el-table-column>
                            <el-table-column label="最后更新时间" width="180">
                                <template slot-scope="scope">
                                    {{ formatDate(scope.row.lastUpdateTime) }}
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-card>
                </div>
            </el-card>
            
            <el-dialog title="异常记录详情" :visible.sync="errorDialogVisible" width="60%">
                <div v-if="currentError">
                    <h3>表: {{ currentError.tableName }}</h3>
                    <el-table :data="currentError.errors" border style="width: 100%">
                        <el-table-column type="index" width="50"></el-table-column>
                        <el-table-column prop="errorDetail" label="错误详情" min-width="250"></el-table-column>
                        <el-table-column label="错误时间" width="180">
                            <template slot-scope="scope">
                                {{ formatDate(scope.row.errorTime) }}
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </el-dialog>
        </div>
    `
}; 