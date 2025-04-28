<template>
  <div class="page-layout">
    <!-- 左侧筛选栏 -->
    <el-aside v-if="allKeywords && allKeywords.length > 0" class="sidebar">
    
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
    </el-aside>
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
      <p class="result-count">Total {{ filteredResults.length }} results</p>
    </div>
    </el-card>
    <div v-if="filteredResults && filteredResults.length" class="mt-8 space-y-6">
      <SearchResult
        v-for="(result, index) in paginatedResults"
        :key="index"
        :result="result"
      />
      <el-pagination
        :current-page="currentPage"
        :page-size="itemsPerPage"
        :total="filteredResults.length"
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
import axios from 'axios'
import { useRoute } from 'vue-router';
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import SearchResult from '@/components/SearchResult.vue'
const aiResult = ref("");
const searchAI = async () => {
  if (!query.value) return;  // 没有输入就别请求了
  try {
    const endpoint = "https://hkust.azure-api.net/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-10-21";

    const response = await axios.post(
      endpoint,
      {
        messages: [
          { role: "user", content: query.value }
        ],
        max_tokens: 100,
        temperature: 0.5,
      },
      {
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer 4aa4a96857d34b759c149840af2d4641`,
        },
      }
    );

    console.log('API Response:', response.data);
    aiResult.value = response.data.choices[0].message.content;
  } catch (error) {
    console.error("Error fetching from Azure:", error);
    aiResult.value = "Failed to fetch AI response.";
  }
};
const authStore = useAuthStore();
const route = useRoute();
const queryFromUrl = computed(() => route.query.query); // 从 URL 获取 query 参数
const query = ref(''); // 用于存储输入框的查询词

// 如果 URL 参数不存在，可以使用输入框的值
if (!queryFromUrl.value) {
  query.value = ''; // 允许从输入框中获取查询
} else {
  query.value = queryFromUrl.value; // 如果从 URL 参数获取
}

const results = ref([])
const searched = ref(false)
const history = ref([])
const itemsPerPage = 10;
const currentPage = ref(1);
const sortBy = ref('relevance')

const allKeywords = computed(() => {
  const keywordFreqMap = new Map();
  if (results.value.length != 0) {
    results.value.forEach(result => {
      if (result.keywords && Array.isArray(result.keywords)) {
        result.keywords.forEach(keyword => {
          if (keyword) {
            const currentFreq = keywordFreqMap.get(keyword) || 0;
            keywordFreqMap.set(keyword, currentFreq + 1);
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
  }
  return []
})
watch(query, (newQuery) => {
  if (newQuery && newQuery.trim()) {
    searchAI();  // 只要有新的query，就调用
  }
});
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

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
const paginatedResults = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  return filteredResults.value.slice(start, end);
});
const filteredResults = computed(() => {
  if (!results.value || results.value.length === 0) {
    return [];
  }
  let filtered = results.value

  if (selectedKeywords.value.length > 0) {
    filtered = filtered.filter(item =>
      selectedKeywords.value.some(k =>
        item.keywords.some(termObj => termObj === k)
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

onMounted(() => {
  if (query.value) {
    search()  // 如果有查询参数，则执行搜索
  }
  if (authStore.isLoggedIn) {
    fetchHistory();
  }
  window.addEventListener('scroll', handleScroll)
})
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
  width: 250px;
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
  margin-left: auto; 
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