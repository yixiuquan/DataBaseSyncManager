// 首页组件
const Home = {
    data() {
        return {
            databaseCount: 0,
            taskCount: 0,
            runningTaskCount: 0,
            loading: true
        };
    },
    created() {
        this.$emit('update-active-index', '/');
        this.fetchData();
    },
    methods: {
        fetchData() {
            this.loading = true;
            Promise.all([
                api.database.getAllDatabases(),
                api.syncTask.getAllSyncTasks()
            ]).then(([databasesRes, tasksRes]) => {
                const databases = databasesRes.data || [];
                const tasks = tasksRes.data || [];
                
                this.databaseCount = databases.length;
                this.taskCount = tasks.length;
                this.runningTaskCount = tasks.filter(task => task.status === 1).length;
                
                this.loading = false;
            }).catch(() => {
                this.loading = false;
            });
        }
    },
    template: `
        <div>
            <h2>FlinkCDC管理平台</h2>
            <el-row :gutter="20">
                <el-col :span="8">
                    <el-card shadow="hover" class="custom-card">
                        <div class="stat-card">
                            <div class="stat-value">{{ databaseCount }}</div>
                            <div class="stat-label">数据库连接</div>
                        </div>
                        <div style="text-align: right; margin-top: 15px;">
                            <el-button size="mini" type="primary" @click="$router.push('/databases')">
                                查看详情
                                <i class="el-icon-arrow-right el-icon--right"></i>
                            </el-button>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="custom-card">
                        <div class="stat-card">
                            <div class="stat-value">{{ taskCount }}</div>
                            <div class="stat-label">同步任务</div>
                        </div>
                        <div style="text-align: right; margin-top: 15px;">
                            <el-button size="mini" type="primary" @click="$router.push('/tasks')">
                                查看详情
                                <i class="el-icon-arrow-right el-icon--right"></i>
                            </el-button>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card shadow="hover" class="custom-card">
                        <div class="stat-card">
                            <div class="stat-value">{{ runningTaskCount }}</div>
                            <div class="stat-label">运行中任务</div>
                        </div>
                        <div style="text-align: right; margin-top: 15px;">
                            <el-button size="mini" type="primary" @click="$router.push('/tasks')">
                                查看详情
                                <i class="el-icon-arrow-right el-icon--right"></i>
                            </el-button>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
            
            <el-card shadow="hover" class="custom-card" style="margin-top: 20px;">
                <div slot="header" class="card-header">
                    <span>快速操作</span>
                </div>
                <el-row :gutter="20">
                    <el-col :span="8">
                        <el-button type="primary" icon="el-icon-plus" @click="$router.push('/databases/add')">
                            添加数据库连接
                        </el-button>
                    </el-col>
                    <el-col :span="8">
                        <el-button type="success" icon="el-icon-plus" @click="$router.push('/tasks/add')">
                            创建同步任务
                        </el-button>
                    </el-col>
                </el-row>
            </el-card>
            
            <el-card shadow="hover" class="custom-card" style="margin-top: 20px;">
                <div slot="header" class="card-header">
                    <span>使用指南</span>
                </div>
                <el-steps :active="1" simple>
                    <el-step title="添加数据库" description="添加源数据库和目标数据库连接信息"></el-step>
                    <el-step title="创建同步任务" description="选择源表和目标表，配置同步选项"></el-step>
                    <el-step title="启动任务" description="启动任务并监控同步进度"></el-step>
                </el-steps>
            </el-card>
        </div>
    `
}; 