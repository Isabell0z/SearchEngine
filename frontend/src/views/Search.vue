<template>
  <div class="page-layout">
    <!-- 左侧筛选栏 -->
    <aside class="sidebar">
      <h3>Keywords</h3>
      <div v-for="keyword in allKeywords" :key="keyword">
        <label>
          <input
            type="checkbox"
            :value="keyword"
            v-model="selectedKeywords"
          />
          {{ keyword }}
        </label>
      </div>
    </aside>
    <main class="main-content">
      <button
    v-show="showBackToTop"
    class="back-to-top"
    @click="scrollToTop"
  >
    Top
  </button>
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
      <div class="sort-buttons">
        <button
        :class="{ active: sortBy === 'relevance' }"
        @click="sortBy = 'relevance'"
      >
      Sorted by relevance
      </button>
      <button
        :class="{ active: sortBy === 'credibility' }"
        @click="sortBy = 'credibility'"
      >
      Sorted by authority
      </button>
      <p class="result-count">Total {{ results.length }} results</p>
    </div>
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
</main>
</div>

</template>

<script setup>
import { computed } from 'vue';
import { watch } from 'vue'
import { ref, onMounted, onUnmounted } from 'vue'
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
const sortBy = ref('relevance')
const allKeywords = computed(() => {
  const keywordFreqMap = new Map();

  // 统计所有 term 的 frequency
  results.value.forEach(result => {
    if (result.content && Array.isArray(result.content.term_freq_list)) {
      result.content.term_freq_list.forEach(keyword => {
        if (keyword.term) {
          const currentFreq = keywordFreqMap.get(keyword.term) || 0;
          keywordFreqMap.set(keyword.term, currentFreq + (keyword.frequency || 0));
        }
      });
    }
  });

  // 排序，取前 10 个高频关键词
  const topKeywords = Array.from(keywordFreqMap.entries())
    .sort((a, b) => b[1] - a[1]) // 按 frequency 降序
    .slice(0, 10)
    .map(([term, _]) => term);   // 只要 term，不要频率

  const sortedKeywords = [...topKeywords].sort()
  return Array.from(sortedKeywords)  // 将 Set 转换为数组并返回
})
watch(results, (newResults) => {
  console.log("Results updated:", newResults)
  console.log("All keywords updated:", allKeywords.value)  // 打印新的 allKeywords
})
const selectedKeywords = ref([])
const showBackToTop = ref(false)

const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
const paginatedResults = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  return filteredResults.value.slice(start, end);
});
const filteredResults = computed(() => {
  let filtered = results.value

  if (selectedKeywords.value.length > 0) {
    filtered = filtered.filter(item =>
      selectedKeywords.value.every(k =>
        item.content.term_freq_list.some(termObj => termObj.term === k)
      )
  )
}

  if (sortBy.value === 'relevance') {
    return [...filtered].sort((a, b) => b.score - a.score)
  } else {
    return [...filtered].sort((a, b) => b.content.page_rank - a.content.page_rank)
  }
})
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
.page-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 0 0px;
}
.sidebar {
  width: 200px;
  min-width: 180px;
  padding: 16px;
  border-right: 1px solid #eee;
  background-color: #fafafa;
  font-size: 14px;
}
.main-content {
  flex: 1;
}
.sort-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 1rem;
}

.search-container {
  display: flex;
  align-items: center;
}
.sort-buttons .result-count {
  margin-left: auto; /* 把结果数量挤到最右边 */
  font-size: inherit;
  color: #555;
}
.result-count {
  color: gray;
  font-size: 14px;
  margin-left: 30px;
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 1rem;
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
.sort-buttons button {
  background: none;
  border: none;
  font-size: 15px;
  color: grey;
  cursor: pointer;
  margin-right: 12px;
  padding: 4px 8px;
  margin-top: 10px;
}
</style>

<style scoped>
/* 您可以在这里添加自定义分页样式 */
.el-pagination {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}

.sort-buttons button.active {
  color: black;
  font-weight: bold;
}

.back-to-top {
  position: fixed;
  bottom: 50px;
  right: 30px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  z-index: 999;
  transition: opacity 0.3s ease;
}

.back-to-top:hover {
  background-color: #66b1ff;
}
</style>