// 任务列表组件
const TaskList = {
    data() {
        return {
            tasks: [],
            loading: true,
            search: '',
            databases: {},
            statusOptions: [
                { value: '', label: '全部状态' },
                { value: 0, label: '已停止' },
                { value: 1, label: '运行中' },
                { value: 2, label: '异常' }
            ],
            statusFilter: '',
            timer: null
        };
    },
    created() {
        this.$emit('update-active-index', '/tasks');
        this.fetchData();
        
        // 设置定时器，每30秒刷新一次数据
        this.timer = setInterval(() => {
            this.fetchData(false);
        }, 30000);
    },
    beforeDestroy() {
        // 组件销毁前清除定时器
        if (this.timer) {
            clearInterval(this.timer);
        }
    },
    methods: {
        fetchData(showLoading = true) {
            if (showLoading) {
                this.loading = true;
            }
            
            Promise.all([
                api.syncTask.getAllSyncTasks(),
                api.database.getAllDatabases()
            ]).then(([tasksRes, databasesRes]) => {
                this.tasks = tasksRes.data || [];
                
                // 将数据库列表转换为对象，方便查询
                const databases = databasesRes.data || [];
                this.databases = {};
                databases.forEach(db => {
                    this.databases[db.id] = db;
                });
                
                this.loading = false;
            }).catch(() => {
                this.loading = false;
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
        handleStart(row) {
            this.$confirm('确认启动该同步任务？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'info'
            }).then(() => {
                this.loading = true;
                api.syncTask.startTask(row.id).then(() => {
                    this.$message.success('任务启动成功');
                    this.fetchData();
                }).catch(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        handleStop(row) {
            this.$confirm('确认停止该同步任务？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.loading = true;
                api.syncTask.stopTask(row.id).then(() => {
                    this.$message.success('任务停止成功');
                    this.fetchData();
                }).catch(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        handleDelete(row) {
            this.$confirm('确认删除该同步任务？删除后不可恢复。', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.loading = true;
                api.syncTask.deleteSyncTask(row.id).then(() => {
                    this.$message.success('删除成功');
                    this.fetchData();
                }).catch(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        formatSyncType(type) {
            return type === 0 ? '全量同步' : '增量同步';
        },
        formatDate(date) {
            if (!date) return '-';
            return new Date(date).toLocaleString();
        }
    },
    computed: {
        filteredTasks() {
            const search = this.search.toLowerCase();
            const statusFilter = this.statusFilter;
            
            return this.tasks.filter(task => {
                const matchSearch = task.taskName.toLowerCase().includes(search);
                const matchStatus = statusFilter === '' || task.status === Number(statusFilter);
                return matchSearch && matchStatus;
            });
        }
    },
    template: `
        <div>
            <el-card shadow="hover" class="custom-card">
                <div slot="header" class="card-header">
                    <span>同步任务管理</span>
                    <el-button type="primary" size="small" @click="$router.push('/tasks/add')">
                        创建同步任务
                    </el-button>
                </div>
                
                <div class="table-header">
                    <div class="search-box">
                        <el-input
                            v-model="search"
                            placeholder="搜索任务名称..."
                            prefix-icon="el-icon-search"
                            clearable
                            style="width: 250px; margin-right: 10px;">
                        </el-input>
                        <el-select v-model="statusFilter" placeholder="状态筛选" style="width: 120px">
                            <el-option
                                v-for="item in statusOptions"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value">
                            </el-option>
                        </el-select>
                    </div>
                    <el-button type="primary" icon="el-icon-refresh" circle @click="fetchData"></el-button>
                </div>
                
                <el-table
                    :data="filteredTasks"
                    v-loading="loading"
                    border
                    style="width: 100%">
                    <el-table-column
                        type="index"
                        width="30">
                    </el-table-column>
                    <el-table-column
                        prop="taskName"
                        label="任务名称"
                        min-width="100">
                    </el-table-column>
                    <el-table-column
                        label="源库"
                        min-width="105">
                        <template slot-scope="scope">
                            {{ getDatabaseName(scope.row.sourceDbId) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="目标库"
                        min-width="105">
                        <template slot-scope="scope">
                            {{ getDatabaseName(scope.row.targetDbId) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="同步类型"
                        width="80">
                        <template slot-scope="scope">
                            {{ formatSyncType(scope.row.syncType) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="任务状态"
                        width="80">
                        <template slot-scope="scope">
                            <span :class="['task-status', getStatusClass(scope.row.status)]">
                                {{ formatStatus(scope.row.status) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="创建时间"
                        width="135">
                        <template slot-scope="scope">
                            {{ formatDate(scope.row.createTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="启动时间"
                        width="135">
                        <template slot-scope="scope">
                            {{ formatDate(scope.row.taskStartTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="操作"
                        fixed="right"
                        width="355">
                        <template slot-scope="scope">
                            <el-button size="mini" type="primary" @click="$router.push('/tasks/monitor/' + scope.row.id)">监控</el-button>
                            <el-button size="mini" type="success" @click="handleStart(scope.row)" :disabled="scope.row.status === 1">启动</el-button>
                            <el-button size="mini" type="warning" @click="handleStop(scope.row)" :disabled="scope.row.status !== 1">停止</el-button>
                            <el-button size="mini" type="info" @click="$router.push('/tasks/edit/' + scope.row.id)" :disabled="scope.row.status === 1">编辑</el-button>
                            <el-button size="mini" type="danger" @click="handleDelete(scope.row)" :disabled="scope.row.status === 1">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
        </div>
    `
}; 