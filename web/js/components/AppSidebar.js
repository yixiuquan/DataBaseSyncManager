// 侧边栏组件
Vue.component('app-sidebar', {
    props: {
        activeIndex: {
            type: String,
            default: '/'
        }
    },
    data() {
        return {
            defaultActive: this.activeIndex
        };
    },
    watch: {
        activeIndex(newVal) {
            this.defaultActive = newVal;
        }
    },
    template: `
        <el-menu
            :default-active="defaultActive"
            class="el-menu-vertical"
            router>
            <el-menu-item index="/">
                <i class="el-icon-s-home"></i>
                <span slot="title">首页</span>
            </el-menu-item>
            <el-submenu index="database">
                <template slot="title">
                    <i class="el-icon-coin"></i>
                    <span>数据库管理</span>
                </template>
                <el-menu-item index="/databases">数据库列表</el-menu-item>
                <el-menu-item index="/databases/add">添加数据库</el-menu-item>
            </el-submenu>
            <el-submenu index="task">
                <template slot="title">
                    <i class="el-icon-s-operation"></i>
                    <span>同步任务管理</span>
                </template>
                <el-menu-item index="/tasks">任务列表</el-menu-item>
                <el-menu-item index="/tasks/add">创建任务</el-menu-item>
            </el-submenu>
        </el-menu>
    `
}); 