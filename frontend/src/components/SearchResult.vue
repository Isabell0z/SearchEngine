<!-- src/components/SearchResult.vue -->
<template>
  <el-card shadow="hover" class="p-4 rounded-lg border border-gray-200 w-full">
  <template #header>
    <el-row :gutter="20" class="mb-6">
    <el-col :span="20">
      <el-link
        :href="result.content.url"
        target="_blank"
        class="text-10xl font-black text-blue-600 hover:text-blue-800 transition-colors duration-200 truncate"
      >
        {{ result.content.title }}
      </el-link>
    </el-col>
    <el-col :span="4" class="text-right">
      <!-- Score (right side) -->
      <span class="text-xs font-thin text-gray-300 ml-auto">
         Score: {{ result.score }}
      </span>
    </el-col>
  </el-row>
  <el-row :gutter="20" class="mb-6">
    <el-col :span="16" class="text-right">
      <span class="text-xs font-thin text-gray-300 ml-auto">
        Last modified: {{  formatTime(result.content.last_modify_time) || 'Unknown date' }}
      </span>
    </el-col>
    <el-col :span="4" class="text-right">
      <span class="text-xs font-thin text-gray-300 ml-auto">
        Size: {{ result.content.size ? result.content.size + ' chars' : 'Unknown size' }}
      </span>
    </el-col>
    <el-col :span="4" class="text-right">
      <span class="text-xs font-thin text-gray-300 ml-auto">
        Page-rank: {{ result.content.page_rank }}
      </span>
    </el-col>
  </el-row>
  </template>

    <div class="mb-4 text-sm text-gray-800">
      <strong>Top Keywords:</strong>
      <div class="flex flex-wrap gap-2">
        <el-tag
          v-for="(freq, word) in result.content.keywords"
          :key="word"
          type="success"
          size="small"
          class="mb-1"
        >
           {{ freq }}
        </el-tag>
      </div>
    </div>

    <div v-if="result.content.parent_links.length" class="mt-4">
      <strong>Parent Links:</strong>
      <ul class="ml-4 text-sm text-blue-600 list-disc">
        <li v-for="(link, i) in result.content.child_links" :key="'parent-' + i">
          <template v-if="link.link">
            <el-link 
              :href="link.link" 
              target="_blank" 
              class="hover:text-blue-800"
            >
              {{ link.title || 'empty' }}
            </el-link>
          </template>
          <template v-else>
            empty
          </template>
        </li>
      </ul>
    </div>

    <div v-if="result.content.child_links.length" class="mt-4">
      <strong>Child Links:</strong>
      <ul class="ml-4 text-sm text-blue-600 list-disc">
        <li v-for="(link, i) in result.content.child_links" :key="'child-' + i">
          <template v-if="link.link">
            <el-link 
              :href="link.link" 
              target="_blank" 
              class="hover:text-blue-800"
            >
              {{ link.title || 'empty' }}
            </el-link>
          </template>
          <template v-else>
            empty
          </template>
        </li>
      </ul>
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
}


.el-link:hover {
  color: #0073e6;
}
</style>
