<template>
    <div class="start-page">
      <h1>Welcome to the Search Engine</h1>
      
      <!-- Search Form -->
      <div class="search-form">
    <el-row :gutter="20" class="mb-6" justify="center" align="middle">
      <!-- 搜索输入框 -->
      <el-col :span="18">
        <el-autocomplete
          v-model="searchQuery"
          :fetch-suggestions="getSuggestions"
          placeholder="Enter your keywords (phrases in double quotes)"
          @keyup.enter="goToSearchPage"
          clearable
          class="search-input"
        />
      </el-col>
      <!-- 搜索按钮 -->
      <el-col :span="3">
        <el-button 
          type="success" 
          @click="goToSearchPage" 
          class="search-button" 
          size="medium"
          style="width: 100%;"
        >
          Start Search
        </el-button>
      </el-col>
    </el-row>
  </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  
  const authStore = useAuthStore();
  const searchQuery = ref(''); // The query entered by the user
  const router = useRouter(); // Use router to navigate to the search page
  const history = ref([]); // To store search history
  
  // Navigate to the search page with the query
  const goToSearchPage = () => {
    if (!searchQuery.value.trim()) {
      alert('Please enter a search query');
      return; // Prevent navigation if query is empty
    }
    
    router.push({ path: '/search', query: { query: searchQuery.value } });
  };
  
  // Fetch search history from the server
  const fetchHistory = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('http://127.0.0.1:5001/history', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + token
        }
      });
  
      if (res.ok) {
        const data = await res.json();
        console.log('search history:', data);
        history.value = data;
      }
    } catch (error) {
      console.error('Failed to fetch history:', error);
    }
  };
  
  // Fetch suggestions based on input
  const getSuggestions = (queryString, cb) => {
    let suggestions = [];
  
    if (queryString === '') {
      suggestions = history.value;
    } else {
      suggestions = history.value.filter(item =>
        item.toLowerCase().includes(queryString.toLowerCase())
      );
    }
  
    const formatted = suggestions.map(item => ({ value: item }));
    cb(formatted);
  };
  
  // On mounted, check if the user is logged in and fetch history
  onMounted(() => {
    if (authStore.isLoggedIn) {
      fetchHistory();
    }
    // Automatically search if query is passed via URL query params
    const urlQuery = new URLSearchParams(window.location.search).get('query');
    if (urlQuery) {
      searchQuery.value = urlQuery;
      goToSearchPage();
    }
  });
  </script>

<script>
export default {
  name: 'StartPage',
}

</script>
  
  <style scoped>
  .start-page {
    text-align: center;
    margin-top: 50px;
  }
  
  .search-form {
    margin-top: 20px;
  }
  
  .search-button {
    padding: 10px 20px;
    background-color: #409EFF;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    margin-left: 10px;
  }
  
  .search-button:hover {
    background-color: #45a049;
  }
  </style>