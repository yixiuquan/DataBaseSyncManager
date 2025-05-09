// 数据库表单组件
const DatabaseForm = {
    props: {
        id: {
            type: String,
            default: null
        }
    },
    data() {
        return {
            form: {
                id: null,
                host: '',
                port: 3306,
                dbName: '',
                username: '',
                password: '',
                param: '',
                status: 1
            },
            rules: {
                host: [
                    { required: true, message: '请输入主机地址', trigger: 'blur' },
                ],
                port: [
                    { required: true, message: '请输入端口', trigger: 'blur' },
                    { type: 'number', message: '端口必须为数字', trigger: 'blur', transform: value => Number(value) }
                ],
                dbName: [
                    { required: true, message: '请输入数据库名称', trigger: 'blur' }
                ],
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' }
                ]
            },
            loading: false,
            isEdit: false
        };
    },
    created() {
        this.$emit('update-active-index', '/databases');
        // 如果有ID参数，说明是编辑模式
        if (this.id) {
            this.isEdit = true;
            this.fetchDatabaseInfo();
        }
    },
    methods: {
        fetchDatabaseInfo() {
            this.loading = true;
            api.database.getDatabaseById(this.id).then(res => {
                const data = res.data;
                this.form = {
                    id: data.id,
                    host: data.host,
                    port: data.port,
                    dbName: data.dbName,
                    username: data.username,
                    password: '', // 不显示密码，密码需要重新输入
                    param: data.param,
                    status: data.status
                };
                this.loading = false;
            }).catch(() => {
                this.loading = false;
                this.$router.push('/databases');
            });
        },
        submitForm() {
            this.$refs.form.validate(valid => {
                if (valid) {
                    this.loading = true;
                    
                    // 转换端口为数字
                    this.form.port = Number(this.form.port);
                    
                    let apiCall;
                    if (this.id) {
                        // 编辑：传递包含 id 和表单数据的新对象
                        apiCall = api.database.updateDatabase({ id: this.id, ...this.form });
                    } else {
                        // 添加：直接传递表单数据
                        apiCall = api.database.addDatabase(this.form);
                    }
                    
                    apiCall.then(() => {
                        this.$message.success(this.id ? '编辑数据库连接成功' : '添加数据库连接成功');
                        this.$router.push('/databases'); // 跳转回列表页
                    }).catch(() => {
                        this.loading = false;
                        // 错误消息已在 API 拦截器中处理
                    });
                }
            });
        },
        resetForm() {
            this.$refs.form.resetFields();
        },
        testConnection() {
            this.$refs.form.validate(valid => {
                if (valid) {
                    this.loading = true;
                    api.database.testConnection({
                        host: this.form.host,
                        port: Number(this.form.port),
                        username: this.form.username,
                        password: this.form.password,
                        dbName: this.form.dbName,
                        param: this.form.param
                    }).then(() => {
                        this.$message.success('数据库连接测试成功');
                        this.loading = false;
                    }).catch(() => {
                        this.loading = false;
                    });
                }
            });
        }
    },
    template: `
        <div>
            <el-card shadow="hover" class="custom-card">
                <div slot="header" class="card-header">
                    <span>{{ isEdit ? '编辑' : '添加' }}数据库连接</span>
                </div>
                
                <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="form-container" v-loading="loading">
                    <el-form-item label="主机地址" prop="host">
                        <el-input v-model="form.host" placeholder="输入数据库主机地址"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="端口" prop="port">
                        <el-input v-model.number="form.port" placeholder="输入数据库端口"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="数据库名称" prop="dbName">
                        <el-input v-model="form.dbName"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="form.username" placeholder="输入数据库用户名"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="密码" prop="password">
                        <el-input v-model="form.password" type="password" placeholder="输入数据库密码" show-password></el-input>
                        <div class="el-form-item-description" v-if="isEdit">
                            <i class="el-icon-info"></i> 出于安全考虑，密码不会显示。如需修改密码，请重新输入新密码；如不修改，请留空。
                        </div>
                    </el-form-item>
                    
                    <el-form-item label="额外参数" prop="param">
                        <el-input v-model="form.param" placeholder="输入额外连接参数（可选）"></el-input>
                        <div class="el-form-item-description">
                            <i class="el-icon-info"></i> 例如：useSSL=false&characterEncoding=UTF-8
                        </div>
                    </el-form-item>
                    
                    <el-form-item label="状态" prop="status">
                        <el-switch
                            v-model="form.status"
                            :active-value="1"
                            :inactive-value="0"
                            active-text="启用"
                            inactive-text="停用">
                        </el-switch>
                    </el-form-item>
                    
                    <el-form-item>
                        <el-button type="primary" @click="submitForm">{{ isEdit ? '更新' : '保存' }}</el-button>
                        <el-button @click="resetForm">重置</el-button>
                        <el-button type="success" @click="testConnection">测试连接</el-button>
                        <el-button @click="$router.push('/databases')">返回</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
        </div>
    `
}; 