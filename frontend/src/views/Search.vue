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
    <div v-if="aiResult" class="mt-8">
      <el-card class="p-4 rounded-lg border border-gray-200">
        <h3 class="text-l font-semibold text-gray-800">AI Response</h3>
        <p class="text-gray-600 mt-4">{{ aiResult }}</p>
      </el-card>
    </div>
    <div v-if="results && results.length" class="mt-8 space-y-6">
      <SearchResult
        v-for="(result, index) in paginatedResults"
        :key="index"
        :result="result"
      />
      <el-pagination
        :current-page="currentPage"
        :page-size="itemsPerPage"
        :total="results.length"
        @current-change="handlePageChange"
        layout="prev, pager, next, jumper"
        class="flex justify-center mt-6"
      />
    </div>

    <el-empty v-else-if="searched" description="No results found." class="mt-10" />
  </div>

</template>

<script setup>
import { computed } from 'vue';
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import SearchResult from '@/components/SearchResult.vue'
const aiResult = ref("AI result");
const authStore = useAuthStore();
const query = ref('')
const results = ref([])
const searched = ref(false)
const history = ref([])
const itemsPerPage = 10;
const currentPage = ref(1);
const paginatedResults = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  return results.value.slice(start, end);
});
const handlePageChange = (page) => {
  currentPage.value = page;
  window.scrollTo(0, 0);
};
const getSuggestions = (queryString, cb) => {
  let suggestions = []

  if (queryString === '') {
    suggestions = history.value
  } else {
    suggestions = history.value.filter(item =>
      item.toLowerCase().includes(queryString.toLowerCase())
    )
  }

  // 转换为 Element Plus 可识别格式
  const formatted = suggestions.map(item => ({ value: item }))
  cb(formatted)
}

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
      console.log('search history:', data)
      history.value = data
    }
  } catch (error) {
    console.error('Failed to fetch history:', error)
  }
}

onMounted(() => {
  if (authStore.isLoggedIn) {
    fetchHistory();
  }
});

const search = async () => {
  try {
    const token = localStorage.getItem('token');
    const res = await fetch('http://127.0.0.1:5001/search', {
      method: 'POST', // 使用 POST 方法
      headers: {
        'Content-Type': 'application/json', // 设置请求体类型为 JSON
        ...(authStore.isLoggedIn && { 'Authorization': 'Bearer ' + token })
      },
      body: JSON.stringify({ data: query.value }) // 发送数据，将 query.value 包装为 JSON 格式
    });
    console.log('Token sent:', authStore.isLoggedIn ? token : '[Not logged in]');
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

.el-autocomplete-suggestion {
  z-index: 9999 !important;
}

</style>

<style scoped>
/* 您可以在这里添加自定义分页样式 */
.el-pagination {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}
</style>