<template>
  <div class="login-container">
    <el-card class="box-card">
      <h2>Login</h2>
      <el-form :model="form" :rules="rules" ref="form" label-width="150px" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="Username" prop="username" :rules="usernameRules">
          <el-input v-model="username" placeholder="Enter your username" />
        </el-form-item>
        <el-form-item label="Password" prop="password" :rules="passwordRules">
          <el-input v-model="password" type="password" placeholder="Enter your password" />
        </el-form-item>
        <el-row class="button-group" justify="center">
          <el-col :span="24">
            <el-button type="primary" native-type="submit" class="button">Login</el-button>
          </el-col>
          <el-col :span="24">
            <el-button type="primary" @click="goToRegister" class="button">Register</el-button>
          </el-col>
        </el-row>
      </el-form>
      <el-button type="text" @click="goToSearch" class="back-to-search">Back to search engine</el-button>
    </el-card>
  </div>
</template>
  
  <script>
import { ref } from 'vue';  // Import ref for reactive state
import { useAuthStore } from '@/stores/auth';
import { ElMessage } from 'element-plus'; 
import { useRouter } from 'vue-router';

export default {
  name: 'LoginPage',
  setup() {
    // Pinia store
    const authStore = useAuthStore();
    const router = useRouter();
    // Reactive state
    const username = ref('');
    const password = ref('');

    // Methods
    const handleLogin = async () => {
      try {
        const res = await fetch('http://127.0.0.1:5001/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: username.value, password: password.value })
        });
        const data = await res.json();
        console.log('data sent:', data);
        if (res.ok && data.access_token) {
          authStore.login();  // Assuming the `login` method updates the Pinia store state
          localStorage.setItem('token', data.access_token);
          console.log('Token sent:', localStorage.getItem('token'));
          router.push('/search');
        } else {
          ElMessage.error('Wrong username or password'); 
        }
      } catch (e) {
        console.error('Login failure', e);
        ElMessage.error('Failure');  // 使用 ElMessage 来显示错误消息
      }
    };

    const goToSearch = () => {
      router.push('/search');
    };

    const goToRegister = () => {
      router.push('/register');
    };

    return {
      username,
      password,
      handleLogin,
      goToSearch,
      goToRegister
    };
  }
};
</script>
  
<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}

.box-card {
  width: 400px;
  padding: 20px;
  border-radius: 10px;
}



.el-form-item {
  margin-bottom: 20px;
}

.button-group {
  display: flex;
  gap: 20px; /* Adds space between the buttons */
}

.button {
  width: 100%;
}

.back-to-search {
  margin-top: 20px;
  display: block;
  width: 100%;
  text-align: center;
}
</style>