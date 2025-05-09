import './components/AppHeader.js';
import './components/AppSidebar.js';
import './views/Home.js';
import './views/DatabaseList.js';
import './views/DatabaseForm.js';
import './views/TaskList.js';
import './views/TaskForm.js';
import './views/TaskMonitor.js';

// 定义路由
const routes = [
    { path: '/', component: Home },
    { path: '/databases', component: DatabaseList },
    { path: '/databases/add', component: DatabaseForm },
    { path: '/databases/edit/:id', component: DatabaseForm, props: true },
    { path: '/tasks', component: TaskList },
    { path: '/tasks/add', component: TaskForm },
    { path: '/tasks/edit/:id', component: TaskForm, props: true },
    { path: '/tasks/monitor/:id', component: TaskMonitor, props: true }
];

// 创建路由实例
const router = new VueRouter({
    routes
});

// 创建Vue应用
new Vue({
    el: '#app',
    router,
    data: {
        activeIndex: '/'
    },
    template: `
        <div>
            <app-header></app-header>
            <el-container style="height: calc(100vh - 60px);">
                <el-aside width="200px" class="sidebar">
                    <app-sidebar :activeIndex="activeIndex"></app-sidebar>
                </el-aside>
                <el-main class="main-content">
                    <router-view @update-active-index="updateActiveIndex"></router-view>
                </el-main>
            </el-container>
        </div>
    `,
    methods: {
        updateActiveIndex(index) {
            this.activeIndex = index;
        }
    }
}); 