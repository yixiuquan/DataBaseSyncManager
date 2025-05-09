// 数据库列表组件
const DatabaseList = {
    data() {
        return {
            databases: [],
            loading: true,
            search: '',
            dialogVisible: false,
            currentDatabase: null
        };
    },
    created() {
        this.$emit('update-active-index', '/databases');
        this.fetchDatabases();
    },
    methods: {
        fetchDatabases() {
            this.loading = true;
            api.database.getAllDatabases().then(res => {
                this.databases = res.data || [];
                this.loading = false;
            }).catch(() => {
                this.loading = false;
            });
        },
        handleStatusChange(row) {
            const newStatus = row.status === 1 ? 0 : 1;
            api.database.updateStatus(row.id, newStatus).then(() => {
                row.status = newStatus;
                this.$message.success(`数据库连接${newStatus === 1 ? '启用' : '停用'}成功`);
            });
        },
        handleDelete(row) {
            this.$confirm('确认删除该数据库连接？删除后不可恢复。', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                api.database.deleteDatabase(row.id).then(() => {
                    this.$message.success('删除成功');
                    this.fetchDatabases();
                });
            }).catch(() => {});
        },
        handleTest(row) {
            this.loading = true;
            api.database.testConnection({
                id: row.id,
                host: row.host,
                port: row.port,
                username: row.username,
                password: '', // 密码已加密存储，后端会处理
                param: row.param
            }).then(res => {
                this.$message.success('数据库连接测试成功');
                this.loading = false;
            }).catch(() => {
                this.loading = false;
            });
        },
        formatStatus(row) {
            return row.status === 1 ? '启用' : '停用';
        },
        formatDate(date) {
            if (!date) return '-';
            return new Date(date).toLocaleString();
        },
        handleDetail(row) {
            this.currentDatabase = row;
            this.dialogVisible = true;
            this.loading = true;
            api.database.getDatabaseTables(row.id).then(res => {
                this.currentDatabase.tables = (res.data || []).map(tableName => ({ name: tableName }));
                this.loading = false;
            }).catch(() => {
                this.loading = false;
            });
        }
    },
    computed: {
        filteredDatabases() {
            const search = this.search.toLowerCase();
            return this.databases.filter(db => {
                return db.host.toLowerCase().includes(search) || 
                       (db.param && db.param.toLowerCase().includes(search));
            });
        }
    },
    template: `
        <div>
            <el-card shadow="hover" class="custom-card">
                <div slot="header" class="card-header">
                    <span>数据库连接管理</span>
                    <el-button type="primary" size="small" @click="$router.push('/databases/add')">
                        添加数据库连接
                    </el-button>
                </div>
                
                <div class="table-header">
                    <div class="search-box">
                        <el-input
                            v-model="search"
                            placeholder="搜索数据库主机..."
                            prefix-icon="el-icon-search"
                            clearable
                            style="width: 250px">
                        </el-input>
                    </div>
                </div>
                
                <el-table
                    :data="filteredDatabases"
                    v-loading="loading"
                    border
                    style="width: 100%">
                    <el-table-column
                        type="index"
                        width="30">
                    </el-table-column>
                    <el-table-column
                        prop="host"
                        label="主机地址"
                        min-width="80">
                    </el-table-column>
                    <el-table-column
                        prop="port"
                        label="端口"
                        width="70">
                    </el-table-column>
                    <el-table-column
                        prop="dbName"
                        label="数据库名称"
                        min-width="70">
                    </el-table-column>
                    <el-table-column
                        prop="username"
                        label="用户名"
                        width="80">
                    </el-table-column>
                    <el-table-column
                        prop="param"
                        label="参数"
                        min-width="200">
                        <template slot-scope="scope">
                            {{ scope.row.param || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        prop="status"
                        label="状态"
                        width="70">
                        <template slot-scope="scope">
                            <span :class="['db-status-icon', scope.row.status === 1 ? 'db-status-enabled' : 'db-status-disabled']"></span>
                            {{ formatStatus(scope.row) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="操作"
                        width="350">
                        <template slot-scope="scope">
                            <el-button size="mini" type="primary" @click="handleDetail(scope.row)">查看</el-button>
                            <el-button size="mini" type="success" @click="handleTest(scope.row)">测试连接</el-button>
                            <el-button size="mini" type="warning" @click="$router.push('/databases/edit/' + scope.row.id)">编辑</el-button>
                            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
                        </template>
                    </el-table-column>
                    <el-table-column
                        width="70">
                        <template slot-scope="scope">
                            <el-switch
                                v-model="scope.row.status"
                                :active-value="1"
                                :inactive-value="0"
                                active-color="#13ce66"
                                inactive-color="#ff4949"
                                @change="handleStatusChange(scope.row)">
                            </el-switch>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
            
            <el-dialog title="数据库详情" :visible.sync="dialogVisible" width="60%">
                <div v-if="currentDatabase">
                    <el-descriptions title="基本信息" :column="2" border>
                        <el-descriptions-item label="数据库名称">{{ currentDatabase.dbName }}</el-descriptions-item>
                        <el-descriptions-item label="主机地址">{{ currentDatabase.host }}</el-descriptions-item>
                        <el-descriptions-item label="端口">{{ currentDatabase.port }}</el-descriptions-item>
                        <el-descriptions-item label="用户名">{{ currentDatabase.username }}</el-descriptions-item>
                        <el-descriptions-item label="参数">{{ currentDatabase.param || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="状态">
                            <span :class="['db-status-icon', currentDatabase.status === 1 ? 'db-status-enabled' : 'db-status-disabled']"></span>
                            {{ formatStatus(currentDatabase) }}
                        </el-descriptions-item>
                        <el-descriptions-item label="创建时间">{{ formatDate(currentDatabase.createTime) }}</el-descriptions-item>
                    </el-descriptions>
                    
                    <div style="margin-top: 20px;">
                        <el-divider content-position="left">数据表</el-divider>
                        <el-table :data="currentDatabase.tables || []" v-loading="loading" border style="width: 100%">
                            <el-table-column type="index" width="50"></el-table-column>
                            <el-table-column prop="name" label="表名" min-width="150"></el-table-column>
                        </el-table>
                    </div>
                </div>
            </el-dialog>
        </div>
    `
}; 