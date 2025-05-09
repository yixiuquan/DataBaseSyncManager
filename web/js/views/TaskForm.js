// 任务表单组件
const TaskForm = {
    props: {
        id: {
            type: String,
            default: null
        }
    },
    data() {
        return {
            loading: true,
            form: {
                taskName: '',
                sourceDbId: '',
                targetDbId: '',
                syncType: 0,
                selectedSourceTables: []
            },
            databases: [],
            sourceTables: [],
            loadingTables: false,
            rules: {
                taskName: [
                    { required: true, message: '请输入任务名称', trigger: 'blur' },
                    { min: 2, max: 50, message: '长度在 2 到 50 个字符之间', trigger: 'blur' }
                ],
                sourceDbId: [
                    { required: true, message: '请选择源数据库', trigger: 'change' }
                ],
                targetDbId: [
                    { required: true, message: '请选择目标数据库', trigger: 'change' }
                ],
                selectedSourceTables: [
                    { type: 'array', required: true, message: '请至少选择一个源表', trigger: 'change' }
                ]
            }
        };
    },
    created() {
        this.$emit('update-active-index', '/tasks');
        this.fetchDatabases();
        
        if (this.id) {
            this.fetchTaskById();
        } else {
            this.loading = false;
        }
    },
    methods: {
        fetchDatabases() {
            api.database.getAllDatabases().then(res => {
                this.databases = res.data || [];
            });
        },
        fetchTaskById() {
            this.loading = true;
            api.syncTask.getSyncTaskById(this.id).then(res => {
                const task = res.data;
                if (task) {
                    // 解析tables字段
                    let selectedTables = [];
                    try {
                        if (task.tables) {
                            const tables = JSON.parse(task.tables);
                            selectedTables = tables.map(item => item.sourceTable);
                        }
                    } catch (e) {
                        console.error('解析tables字段失败:', e);
                    }

                    this.form = {
                        taskName: task.taskName,
                        sourceDbId: task.sourceDbId,
                        targetDbId: task.targetDbId,
                        syncType: task.syncType,
                        selectedSourceTables: selectedTables
                    };
                    
                    if (task.sourceDbId) {
                        this.onSourceDbChange(task.sourceDbId, true);
                    }
                }
                this.loading = false;
            }).catch(() => {
                this.loading = false;
                this.$message.error('获取任务信息失败');
                this.$router.push('/tasks');
            });
        },
        onSourceDbChange(dbId, keepSelectedTables = false) {
            if (!dbId) {
                this.sourceTables = [];
                return;
            }
            
            this.loadingTables = true;
            api.database.getDatabaseTables(dbId).then(res => {
                this.sourceTables = (res.data || []).map(tableName => {
                    return { key: tableName, label: tableName };
                });
                this.loadingTables = false;
            }).catch(() => {
                this.loadingTables = false;
                this.$message.error('获取源数据库表失败');
            });
            
            if (!keepSelectedTables) {
                this.form.selectedSourceTables = [];
            }
        },
        submitForm() {
            this.$refs.form.validate(valid => {
                if (!valid) return;
                
                this.loading = true;
                
                // 构建tables数组
                const tables = this.form.selectedSourceTables.map(tableName => ({
                    sourceTable: tableName,
                    targetTable: tableName
                }));
                
                const payload = {
                    taskName: this.form.taskName,
                    sourceDbId: this.form.sourceDbId,
                    targetDbId: this.form.targetDbId,
                    syncType: this.form.syncType,
                    tables: JSON.stringify(tables),
                    startup_options: JSON.stringify({ type: "initial" })
                };

                let apiCall;
                if (this.id) {
                    apiCall = api.syncTask.updateSyncTask(this.id, payload);
                } else {
                    apiCall = api.syncTask.createSyncTask(payload);
                }
                
                apiCall.then(() => {
                    this.$message.success(this.id ? '编辑任务成功' : '创建任务成功');
                    this.$router.push('/tasks');
                }).catch(() => {
                    this.loading = false;
                    this.$message.error(this.id ? '编辑任务失败' : '创建任务失败');
                });
            });
        },
        resetForm() {
            this.$refs.form.resetFields();
            this.form.selectedSourceTables = [];
        }
    },
    template: `
        <div>
            <el-card shadow="hover" class="custom-card" v-loading="loading">
                <div slot="header" class="card-header">
                    <span>{{ id ? '编辑同步任务' : '创建同步任务' }}</span>
                    <el-button size="small" @click="$router.push('/tasks')">返回</el-button>
                </div>
                
                <el-form ref="form" :model="form" :rules="rules" label-width="120px">
                    <el-form-item label="任务名称" prop="taskName">
                        <el-input v-model="form.taskName" placeholder="请输入任务名称"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="同步类型" prop="syncType">
                        <el-radio-group v-model="form.syncType">
                            <el-radio :label="0">全量同步</el-radio>
                            <el-radio :label="1">增量同步</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    
                    <el-form-item label="源数据库" prop="sourceDbId">
                        <el-select 
                            v-model="form.sourceDbId" 
                            placeholder="请选择源数据库" 
                            style="width: 100%;" 
                            @change="onSourceDbChange">
                            <el-option 
                                v-for="db in databases" 
                                :key="db.id" 
                                :label="db.host + ':' + db.port + '/' + db.dbName" 
                                :value="db.id">
                            </el-option>
                        </el-select>
                    </el-form-item>
                    
                    <el-form-item label="目标数据库" prop="targetDbId">
                        <el-select 
                            v-model="form.targetDbId" 
                            placeholder="请选择目标数据库" 
                            style="width: 100%;">
                            <el-option 
                                v-for="db in databases" 
                                :key="db.id" 
                                :label="db.host + ':' + db.port + '/' + db.dbName" 
                                :value="db.id">
                            </el-option>
                        </el-select>
                    </el-form-item>
                    
                    <el-divider content-position="left">选择源表</el-divider>

                    <el-form-item prop="selectedSourceTables"> 
                        <el-transfer
                            v-model="form.selectedSourceTables"
                            :data="sourceTables"
                            :titles="['可用源表', '已选源表']"
                            filterable
                            :props="{ key: 'key', label: 'label' }"
                            style="width: 100%;">
                        </el-transfer>
                    </el-form-item>
                    
                    <el-form-item style="margin-top: 30px;">
                        <el-button type="primary" @click="submitForm">{{ id ? '保存' : '创建' }}</el-button>
                        <el-button @click="resetForm">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
        </div>
    `
}; 