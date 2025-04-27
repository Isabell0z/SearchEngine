<!-- src/components/SearchResult.vue -->
<template>
  <el-card shadow="hover" class="p-4 rounded-lg border border-gray-200 w-full">
  <template #header>
    <el-row :gutter="20" class="mb-6">
    <el-col :span="16">
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
         Score: {{ result.score.toFixed(4) }}
      </span>
    </el-col>
    <el-col :span="4" class="text-right">
      <span class="text-xs font-thin text-gray-300 ml-auto">
        Pagerank: {{ result.content.page_rank.toFixed(4)}}
      </span>
    </el-col>
  </el-row>
  <el-row :gutter="20" class="mb-6">
    <el-col :span="20" class="text-right">
      <span class="text-xs font-thin text-gray-300 ml-auto">
        Last modified: {{  formatTime(result.content.last_modify_time) || 'Unknown date' }}
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
      <strong>Top Keywords:</strong>
      <div class="flex flex-wrap gap-2 gap-y-10"> 
        <el-tag
          v-for="(item, i) in result.content.term_freq_list"
          :key="i"
          type="success"
          size="small"
          class="mb-1"
        >
          {{ item.term }}: {{ item.frequency }}
        </el-tag>
      </div>
    </div>

    <div v-if="result.content.parent_links.length" class="mt-3">
      <strong>Parent Links:</strong>
      <ul class="ml-4 text-sm text-blue-600 list-disc space-y-1"> 
        <li v-for="(link, i) in result.content.parent_links" :key="'parent-' + i">
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

    <div v-if="result.content.child_links.length" class="mt-1">
      <strong>Child Links:</strong>
      <ul class="ml-4 text-sm text-blue-600 list-disc space-y-1"> 
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
