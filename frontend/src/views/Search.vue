<template>
  <div class="max-w-5xl mx-auto py-10 px-6">
    <el-card class="p-6 shadow-lg">
      <h1 class="text-3xl font-semibold text-center mb-6">Search Engine</h1>
      <el-row :gutter="20" class="mb-6">
        <el-col :span="15">
        
        <el-autocomplete
          v-model="query"
          :fetch-suggestions="getSuggestions"
          placeholder="Enter your keywords (phrases in double quotes)"
          @keyup.enter="search"
          clearable
        >
        </el-autocomplete></el-col>
        <el-col :span="9">
        <el-button type="primary" @click="search"> Search </el-button>
        </el-col>
      </el-row>
    </el-card>

    <div v-if="results && results.length" class="mt-8 space-y-6">
      <SearchResult
        v-for="(result, index) in results"
        :key="index"
        :result="result"
      />
    </div>

    <el-empty v-else-if="searched" description="No results found." class="mt-10" />
  </div>

</template>

<script setup>

import { ref } from 'vue'

import SearchResult from '@/components/SearchResult.vue'

const query = ref('')
const results = ref([])
const searched = ref(false)
const search = async () => {
  try {
    const token = localStorage.getItem('token');
    const res = await fetch('http://127.0.0.1:5001/search', {
      method: 'POST', // 使用 POST 方法
      headers: {
        'Content-Type': 'application/json', // 设置请求体类型为 JSON
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ data: query.value }) // 发送数据，将 query.value 包装为 JSON 格式
    });
    console.log('Token sent:', token);
    console.log('Sending search query:', query.value);
    // 解析返回的 JSON 数据
    const data = await res.json();
    
    // 将返回的数据存储到 results 中
    results.value = data;
    
    // 标记已完成搜索
    searched.value = true;
  } catch (err) {
    console.error('Search request failed:', err); // 处理错误
  }
}
</script>
<script>
  export default {
    name: 'SearchPage',
    data() {
    return {
      query: "",  // 当前输入框的内容
      history: ['111']  // 存储历史搜索记录
    };
  },
  methods: {
    async getSuggestions(query) {
      if (query === "") {
        return this.history;  // 如果没有输入，直接显示历史记录
      } else {
        // 如果有输入，返回符合条件的历史记录（模糊匹配）
        return this.history.filter(item => item.toLowerCase().includes(query.toLowerCase()));
      }
    },
    async fetchHistory() {
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
          console.log('search history:',data);
          this.history = data;  // 存储历史搜索记录
        }
      } catch (error) {
        console.error('Failed to fetch history:', error);
      }
    },
    created() {
    this.fetchHistory();  // 在组件创建时获取历史记录
  }
  }
  
  }

</script>

<style scoped>
.search-container {
  display: flex;
  align-items: center;
}

.search-input {
  flex-grow: 1; /* Allow input to take up remaining space */
  margin-right: 10px; /* Space between input and button */
}

.search-button {
  margin-left: 10px; /* Optional: space between input and button */
}
</style>

  