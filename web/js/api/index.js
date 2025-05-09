// 统一API接口封装
const baseURL = 'http://localhost:8087/yxq';

// 封装axios实例
const request = axios.create({
    baseURL: baseURL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
});

// 请求拦截器
request.interceptors.request.use(
    config => {
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data;
        
        // 处理业务状态码
        if (res.code !== 200) {
            // 显示错误信息
            ELEMENT.Message.error(res.message || '操作失败');
            return Promise.reject(new Error(res.message || '操作失败'));
        } else {
            return res;
        }
    },
    error => {
        // 显示错误信息
        ELEMENT.Message.error(error.message || '请求失败');
        return Promise.reject(error);
    }
);

// API接口定义
const api = {
    // 数据库相关接口
    database: {
        // 获取所有数据库连接
        getAllDatabases() {
            return request({
                url: '/db/getAllDatabases',
                method: 'get'
            });
        },
        
        // 添加数据库连接
        addDatabase(data) {
            return request({
                url: '/db/addDatabase',
                method: 'post',
                data
            });
        },
        
        // 更新数据库连接 (只接受一个包含所有数据的对象)
        updateDatabase(data) {
            return request({
                url: '/db/updateDatabase',
                method: 'post',
                data // 直接发送包含 id 和其他字段的对象
            });
        },
        
        // 删除数据库连接
        deleteDatabase(id) {
            return request({
                url: '/db/deleteDatabase',
                method: 'post',
                params: { id }
            });
        },
        
        // 获取单个数据库连接信息
        getDatabaseById(id) {
            return request({
                url: '/db/getDatabaseById',
                method: 'get',
                params: { id }
            });
        },
        
        // 测试数据库连接
        testConnection(data) {
            return request({
                url: '/db/testConnection',
                method: 'post',
                data
            });
        },
        
        // 获取数据库表列表
        getDatabaseTables(databaseId) {
            return request({
                url: '/db/getAllTables',
                method: 'get',
                params: { databaseId }
            });
        },

        // 获取表字段信息
        getTableColumns(databaseId, tableName) {
            return request({
                url: '/db/getTableColumns',
                method: 'get',
                params: { databaseId, tableName }
            });
        },

        // 修改数据库连接状态
        updateStatus(id, status) {
            return request({
                url: '/db/updateStatus',
                method: 'post',
                data: { id, status }
            });
        }
    },
    
    // 同步任务相关接口
    syncTask: {
        // 获取所有同步任务
        getAllSyncTasks() {
            return request({
                url: '/db/getAllSyncTasks',
                method: 'get'
            });
        },
        
        // 创建同步任务
        createSyncTask(data) {
            return request({
                url: '/db/addSyncTask',
                method: 'post',
                data
            });
        },
        
        // 更新同步任务
        updateSyncTask(id, data) {
            return request({
                url: '/db/updateSyncTask',
                method: 'post',
                data: {
                    id,
                    ...data
                }
            });
        },
        
        // 删除同步任务
        deleteSyncTask(id) {
            return request({
                url: '/db/deleteSyncTask',
                method: 'post',
                params: { id }
            });
        },
        
        // 获取单个同步任务信息
        getSyncTaskById(id) {
            return request({
                url: '/db/getSyncTaskById',
                method: 'get',
                params: { id }
            });
        },
        
        // 启动同步任务
        startTask(id) {
            return request({
                url: '/db/startTask',
                method: 'post',
                params: { id }
            });
        },
        
        // 停止同步任务
        stopTask(id) {
            return request({
                url: '/db/stopTask',
                method: 'post',
                params: { id }
            });
        },
        
        // 获取任务统计信息
        getTaskStatistics(id) {
            return request({
                url: '/db/getTaskStatistics',
                method: 'get',
                params: { id }
            });
        }
    }
};

// 添加SyncException相关API
const syncException = {
    getByTaskId: (taskId) => {
        return axios.get('/exception/getByTaskId', { params: { taskId } });
    },
    getByTaskIdAndTableName: (taskId, tableName) => {
        return axios.get('/exception/getByTaskIdAndTableName', { params: { taskId, tableName } });
    },
    countByTaskId: (taskId) => {
        return axios.get('/exception/countByTaskId', { params: { taskId } });
    },
    countByTaskIdAndTableName: (taskId, tableName) => {
        return axios.get('/exception/countByTaskIdAndTableName', { params: { taskId, tableName } });
    }
};

// 导出api对象
window.api = api;