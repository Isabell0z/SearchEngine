<template>
  <div>
    <el-header v-if="showHeader" class="header-container">
      <el-row type="flex" justify="space-between" align="middle">
        
        <el-col :span="12">
          <span v-if="authStore.isLoggedIn" class="greeting">
            Hi, {{ authStore.username }} 
          </span>
        </el-col>

        <el-col :span="6" class="text-right">
          <el-button @click="login" type="danger" class="login-button">
            {{ authStore.isLoggedIn ? 'Logout' : 'Login' }}
          </el-button>
        </el-col>
      </el-row>
    </el-header>

    <router-view />
  </div>
</template>

<script>
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue';

export default {
  setup() {
    const authStore = useAuthStore();  // 使用 Pinia store
    const route = useRoute();  // 获取当前路由
    const router = useRouter();  // 获取路由实例

    const showHeader = computed(() => {
      return route.path !== '/login';
    });

    const login = () => {
      if (authStore.isLoggedIn) {
        authStore.logout();
        localStorage.removeItem('token');
        ElMessage.success('Logged out successfully');
        if (route.path !== '/search') {
          router.push('/search');
        }
      } else {
        router.push('/login');
      }
    };

    return { authStore, showHeader, login };
  }
}
</script>

<style scoped>
:root {
  --el-header-height: 20px; /* 设置头部的高度为 80px */
}
.header-container {
  padding: 0 0px;
  margin: 0 20px;
  height: var(--el-header-height) !important;
}

.greeting {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.login-button {
  font-weight: bold;
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.login-button:hover {
  background-color: #f44336;
  border-color: #f44336;
}

.text-right {
  text-align: right;
}
</style>