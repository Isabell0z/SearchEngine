<template>
  <div>
    <el-header v-if="showHeader">
      <el-button @click="login" type="danger">
        {{ authStore.isLoggedIn ? 'Logout' : 'Login' }}
      </el-button>
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
.el-header {
  background-color: #f5f5f5;
  padding: 10px;
  text-align: right;
}
</style>
