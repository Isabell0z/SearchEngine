<!-- src/components/SearchResult.vue -->
<template>
  <el-card shadow="hover" class="p-4 rounded-lg border border-gray-200 w-full">
    <template #header>
      <el-row :gutter="20" class="mb-6">
        <el-col :span="16">
          <el-link :href="result.content.url" target="_blank"
            class="text-10xl font-black text-blue-600 hover:text-blue-800 transition-colors duration-200 truncate">
            {{ result.content.title || 'empty' }}
          </el-link>
        </el-col>
        <el-col :span="4" class="text-right">
          <!-- Score (right side) -->
          <span class="text-xs font-thin text-gray-300 ml-auto">
            Score: {{ result.score.toFixed(4) || 'empty' }}
          </span>
        </el-col>
        <el-col :span="4" class="text-right">
          <span class="text-xs font-thin text-gray-300 ml-auto">
            Pagerank: {{ result.content.page_rank.toFixed(4) || 'empty' }}
          </span>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb-6">
        <el-col :span="20" class="text-right">
          <span class="text-xs font-thin text-gray-300 ml-auto">
            Last modified: {{ formatTime(result.content.last_modify_time) || 'Unknown date' }}
          </span>
        </el-col>
        <el-col :span="4" class="text-right">
          <span class="text-xs font-thin text-gray-300 ml-auto">
            Size: {{ result.content.size ? result.content.size + ' Bytes' : 'Unknown size' }}
          </span>
        </el-col>
      </el-row>
    </template>


    <div class="mb-3 text-l text-gray-800">
      <div class="results-container flex items-center justify-between" style="margin: 0.5rem ;">

      <strong>Top Keywords:</strong>
      <div class="flex flex-wrap gap-2 gap-y-10">
        <el-tag v-for="(item, i) in result.content.term_freq_list" :key="i" type="success" size="small" class="mb-1">
          {{ item.term }}: {{ item.frequency }}
        </el-tag>
      </div>
      </div>

    </div>

    <div v-if="result.content.parent_links && result.content.parent_links.length" class="mt-3">
      <div class="results-container flex items-center justify-between" style="margin: 0.5rem;">

        <strong>Parent Links:</strong>&nbsp;&nbsp;
        <ul class="ml-4 text-sm text-blue-600 list-disc space-y-1 child-links-list">
          <li v-for="(link, i) in result.content.parent_links" :key="'parent-' + i" class="inline-block">
            <template v-if="link.link">
              <el-link :href="link.link" target="_blank" class="hover:text-blue-800 smaller-child-link">
                {{ link.title || 'empty' }}
              </el-link>
            </template>
            <template v-else>
              empty
            </template>
          </li>
        </ul>
      </div>
    </div>

    <div v-if="result.content.child_links && result.content.child_links.length" class="mt-1">
      <div class="results-container flex items-center justify-between" style="margin: 0.5rem;">
        <strong class="mr-2">Child Links:</strong>&nbsp;&nbsp;
        <ul class="text-sm text-blue-600 child-links-list">
          <li v-for="(link, i) in result.content.child_links" :key="'child-' + i" class="inline-block">
            <template v-if="link.link">
              <el-link :href="link.link" target="_blank" class="hover:text-blue-800 smaller-child-link">
                {{ link.title || 'empty' }}
                <!-- 当不是最后一个链接时，添加逗号 -->
                <span v-if="i !== result.content.child_links.length - 1">,</span>
              </el-link>
            </template>
            <template v-else>
              empty
            </template>
          </li>
        </ul>
      </div>
    </div>
    <div v-if="result.snippents && result.snippents.length">
      <h3 class="text-lg font-semibold mb-4" style="margin:0.5rem">Highlighted Snippets</h3>
      <div v-for="(snippet, index) in result.snippents" :key="index"
        class="snippet-card mb-4 p-4 rounded-lg shadow-md bg-white">
        <div>&nbsp;&nbsp;&nbsp;&nbsp;... <span v-html="snippet"></span> ...</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
// eslint-disable-next-line no-undef
defineProps({
  result: Object
})
function formatTime(isoTime) {
  const date = new Date(isoTime);
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}
</script>

<style scoped>
/* Additional custom styles */
.el-card {
  background-color: #f9f9f9;
  margin:2 rem;
}


.mb-1{
  margin-right:0.3rem;
}

.mt-4{
  margin: 0;
}

.el-link {
  font-size: 1.25rem;
  /* 增大字体大小 */
  color: #0066cc;
  /* 修改文字颜色 */
  text-decoration: none;
  /* 去除下划线 */
  transition: color 0.3s ease;
  /* 增加颜色过渡效果 */
}

.el-link:hover {
  color: #0073e6;
}

.snippet-card {
  background-color: #f9fafb;
  /* 背景颜色 */
  border: none;
  /* 边框颜色 */
  margin-left : 10px;
  /* 内边距 */
  border-radius: 8px;
  /* 边框圆角 */
  /* box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); 阴影效果 */
  transition: transform 0.3s, box-shadow 0.3s;
}

.results-container {
  display: flex;

  align-items: center;
  /* 垂直居中对齐 */
  margin: 0;
}

.smaller-child-link {
  font-size: 1rem;
  /* 其他样式属性 */
}

/* 新添加的样式 */
.child-links-list {
  list-style-type: none;
  /* 去除黑点 */
  padding: 0;
  /* 去除默认的内边距 */
  display: flex;
  /* 使列表项横向排列 */
  flex-wrap: wrap;
  /* 允许列表项换行 */
  gap: 10px;
  /* 设置列表项之间的间距 */
  margin: 0;
  /* 去除默认的外边距 */
}

.child-links-list li {
  margin: 0;
  /* 去除默认的外边距 */
}

/* 高亮部分样式，字体变红 */
mark {
  color: #e11d48;
  /* 红色字体 */
  background-color: transparent;
  /* 去除背景色 */
  font-weight: bold;
  /* 加粗字体 */
}

.results-container {
  display: flex;
  align-items: center; /* 垂直居中对齐 */
  margin-bottom: 1rem; /* 可选的底部间距 */
}

/* 标题样式 */
h3 {
  font-size: 1rem;
  color: #4b5563;
  /* 深灰色标题 */
  margin-bottom: 1rem;
}
</style>
