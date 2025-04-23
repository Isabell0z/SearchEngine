<!-- src/components/SearchResult.vue -->
<template>
    <el-card shadow="hover">
      <template #header>
        <div class="flex justify-between items-center">
          <span>
            <strong>Score:</strong> {{ result.score }}
          </span>
          <el-link :href="result.content.url" target="_blank">{{ result.content.title }}</el-link>
        </div>
      </template>
  
      <div class="mb-2 text-sm text-gray-600">
        <el-link :href="result.content.url" type="info" target="_blank">{{ result.content.url }}</el-link>
      </div>
  
      <div class="mb-2 text-xs text-gray-500">
        {{ result.content.last_modified || 'Unknown date' }},
        {{ result.content.size ? result.content.size + ' chars' : 'Unknown size' }}
      </div>
  
      <div class="mb-2 text-sm">
        <strong>Top Keywords:</strong>
        <el-tag
          v-for="(freq, word) in result.content.keywords"
          :key="word"
          type="success"
          size="small"
          class="mr-2 mb-1"
        >
          {{ word }} ({{ freq }})
        </el-tag>
      </div>
  
      <div v-if="result.content.parent_links.length">
        <strong>Parent Links:</strong>
        <ul class="ml-4 text-sm text-blue-600 list-disc">
          <li v-for="(link, i) in result.content.parent_links" :key="'p' + i">
            <el-link :href="link" target="_blank">{{ link }}</el-link>
          </li>
        </ul>
      </div>
  
      <div v-if="result.content.child_links.length" class="mt-2">
        <strong>Child Links:</strong>
        <ul class="ml-4 text-sm text-blue-600 list-disc">
          <li v-for="(link, i) in result.content.child_links" :key="'c' + i">
            <el-link :href="link" target="_blank">{{ link }}</el-link>
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
  </script>
  