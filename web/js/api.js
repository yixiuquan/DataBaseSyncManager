// API 基础路径
const BASE_URL = '/api';

// 请求拦截器
axios.interceptors.request.use(
    config => {
        // 在发送请求之前做些什么
        return config;
    },
    error => {
        // 对请求错误做些什么
        return Promise.reject(error);
    }
);

// 响应拦截器
axios.interceptors.response.use(
    response => {
        // 对响应数据做点什么
        if (response.data.code === 200) {
            return response.data;
        } else {
            Element.Message.error(response.data.message || '请求失败');
            return Promise.reject(response.data);
        }
    },
    error => {
        // 对响应错误做点什么
        Element.Message.error('网络异常，请稍后重试');
        return Promise.reject(error);
    }
);

// 数据库管理API
const databaseApi = {
    // 获取所有数据库连接信息
    getAllDatabases() {
        return axios.get(`${BASE_URL}/databases`);
    },
    
    // 根据ID获取数据库连接信息
    getDatabaseById(id) {
        return axios.get(`${BASE_URL}/databases/${id}`);
    },
    
    // 添加数据库连接信息
    addDatabase(database) {
        return axios.post(`${BASE_URL}/databases`, database);
    },
    
    // 更新数据库连接信息
    updateDatabase(database) {
        return axios.put(`${BASE_URL}/databases/${database.id}`, database);
    },
    
    // 删除数据库连接信息
    deleteDatabase(id) {
        return axios.delete(`${BASE_URL}/databases/${id}`);
    },
    
    // 更新数据库连接状态
    updateStatus(id, status) {
        return axios.put(`${BASE_URL}/databases/${id}/status`, { status });
    },
    
    // 测试数据库连接
    testConnection(database) {
        return axios.post(`${BASE_URL}/databases/test`, database);
    },
    
    // 获取数据库中所有表
    getAllTables(databaseId) {
        return axios.get(`${BASE_URL}/databases/${databaseId}/tables`);
    },
    
    // 获取表的列信息
    getTableColumns(databaseId, tableName) {
        return axios.get(`${BASE_URL}/databases/${databaseId}/tables/${tableName}/columns`);
    }
};

// 同步任务API
const syncTaskApi = {
    // 获取所有同步任务
    getAllSyncTasks() {
        return axios.get(`${BASE_URL}/tasks`);
    },
    
    // 根据ID获取同步任务
    getSyncTaskById(id) {
        return axios.get(`${BASE_URL}/tasks/${id}`);
    },
    
    // 添加同步任务
    addSyncTask(syncTask) {
        return axios.post(`${BASE_URL}/tasks`, syncTask);
    },
    
    // 更新同步任务
    updateSyncTask(syncTask) {
        return axios.put(`${BASE_URL}/tasks/${syncTask.id}`, syncTask);
    },
    
    // 删除同步任务
    deleteSyncTask(id) {
        return axios.delete(`${BASE_URL}/tasks/${id}`);
    },
    
    // 启动同步任务
    startTask(id) {
        return axios.post(`${BASE_URL}/tasks/${id}/start`);
    },
    
    // 停止同步任务
    stopTask(id) {
        return axios.post(`${BASE_URL}/tasks/${id}/stop`);
    },
    
    // 获取任务监控统计信息
    getTaskStatistics(id) {
        return axios.get(`${BASE_URL}/tasks/${id}/statistics`);
    }
};

// 导出API
const api = {
    database: databaseApi,
    syncTask: syncTaskApi
}; 