const { defineConfig } = require('@vue/cli-service')
const path = require('path')

module.exports = {
  transpileDependencies: true,
  configureWebpack: {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),  // 配置 @ 为 src 目录的别名
      }
    }
  }
}