const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    // 백엔드(Spring Boot)가 8080을 쓰므로 개발 서버는 3000, API는 백엔드로 프록시
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:8080' }
    }
  }
})
