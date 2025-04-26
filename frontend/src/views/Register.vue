<template>
  <div class="register-container">   
     <el-card class="box-card">
      <h2>Register</h2>
      <el-form :model="form" :rules="rules" ref="form" label-width="150px" label-position="top" @submit.prevent="handleRegister">
        
        <el-form-item label="Username" prop="username" :rules="usernameRules">
          <el-input v-model="form.username" placeholder="Enter your username" />
        </el-form-item>
        
        <el-form-item label="Password" prop="password" :rules="passwordRules">
          <el-input v-model="form.password" type="password" placeholder="Enter your password" />
        </el-form-item>
        
        <el-form-item label="Confirm Password" prop="confirmPassword" :rules="confirmPasswordRules">
          <el-input v-model="form.confirmPassword" type="password" placeholder="Confirm your password" />
        </el-form-item>
      </el-form>
        <el-form-item>
        
            <el-col :span="24">
              <el-button type="primary" @click="handleRegister" class="button">Register</el-button>
            </el-col>
          
          <el-button type="text" @click="goToSearch" class="back-to-search">Back to search engine</el-button>
        </el-form-item>    
    </el-card>
  </div>
</template>
  
  <script>
  export default {
    name: 'RegisterPage',
    data() {
      return {
        form: {
          username: '',
          password: '',
          confirmPassword: ''
        },
        usernameRules: [
          { required: true, message: 'Please enter a username', trigger: 'blur' },
          { min: 3, max: 20, message: 'Username must be between 3 and 20 characters', trigger: 'blur' }
        ],
        passwordRules: [
          { required: true, message: 'Please enter a password', trigger: 'blur' },
          { min: 6, max: 20, message: 'Password must be between 6 and 20 characters', trigger: 'blur' }
        ],
        confirmPasswordRules: [
          { required: true, message: 'Please confirm your password', trigger: 'blur' },
          { validator: this.validateConfirmPassword, trigger: 'blur' }
        ]
      };
    },
    methods: {
      async handleRegister() {
        this.$refs.form.validate(async (valid) => {
          if (valid) {
            const res = await fetch('http://127.0.0.1:5001/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: this.form.username, password: this.form.password })
          });
            console.log('Registering user:', this.form);
            if (res.status === 201) {
              console.log('Registering user:', this.form);
              this.$router.push('/login');
            } else {
              console.log('Registration failed!');
                return false;
            }
            this.$router.push('/login');
          } else {
            console.log('Registration failed!');
            return false;
          }
        });
      },

      validateConfirmPassword(rule, value, callback) {
        if (value !== this.form.password) {
          callback(new Error('The two passwords do not match'));
        } else {
          callback();
        }
      },
      goToSearch() {
        this.$router.push('/search');
      }
    }
  };
  </script>
  
  <style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}

.box-card {
  width: 400px;
  padding: 20px;
}

.back-to-search {
  margin-top: 20px;
  display: block;
  width: 100%;
  text-align: center;
}
const 
.button-group {
  display: flex;
  justify-content: center;  /* Ensure buttons are centered */
}

.button {
  width: 100%; /* Make the button fill the entire width of its parent container */
}
</style>