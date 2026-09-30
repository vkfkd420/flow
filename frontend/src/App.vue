<template>
  <div id="app">
    <header class="header">
      <h1>flow</h1>
      <nav class="tabs" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          role="tab"
          class="tab"
          :class="{ active: current === tab.id }"
          :aria-selected="current === tab.id"
          @click="current = tab.id"
        >{{ tab.label }}</button>
      </nav>
    </header>

    <main>
      <!-- 탭을 바꿔도 입력·결과가 유지되도록 v-show 사용 -->
      <ExtensionPolicy v-show="current === 'policy'" />
      <FileUpload v-show="current === 'upload'" />
    </main>
  </div>
</template>

<script>
import ExtensionPolicy from './components/ExtensionPolicy.vue'
import FileUpload from './components/FileUpload.vue'

export default {
  name: 'App',
  components: {
    ExtensionPolicy,
    FileUpload
  },
  data() {
    return {
      current: 'policy',
      tabs: [
        { id: 'policy', label: '확장자 차단 정책' },
        { id: 'upload', label: '파일 업로드' }
      ]
    }
  }
}
</script>

<style>
:root {
  --text: #222;
  --muted: #777;
  --border: #d9d9d9;
  --bg: #f5f6f8;
  --panel: #fff;
  --chip: #f0f2f5;
  --primary: #3b6fd8;
  --danger: #d6454b;
  --success: #2e8b57;
}
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  background: var(--bg);
  color: var(--text);
  font-family: -apple-system, BlinkMacSystemFont, 'Malgun Gothic', '맑은 고딕', sans-serif;
  font-size: 14px;
}
#app {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px 16px;
}
.header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.header h1 {
  margin: 0;
  font-size: 20px;
}
.tabs {
  display: flex;
  gap: 4px;
}
.tab {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--panel);
  cursor: pointer;
  font-size: 14px;
}
.tab.active {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}
.panel {
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--panel);
}
.title {
  margin: 0 0 4px;
  font-size: 17px;
}
.desc {
  margin: 0 0 18px;
  color: var(--muted);
}
.row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.label {
  flex: 0 0 100px;
  padding-top: 6px;
  font-weight: 600;
}
.field {
  flex: 1;
  min-width: 0;
  padding-top: 6px;
}
.text-input {
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 14px;
}
.btn {
  padding: 7px 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--panel);
  cursor: pointer;
  font-size: 14px;
}
.btn.primary {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}
.btn:disabled,
.tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.state {
  padding: 20px 0;
  color: var(--muted);
}
.state.error,
.message.error {
  color: var(--danger);
}
.message {
  min-height: 20px;
  margin: 8px 0 0;
}
.message.success {
  color: var(--success);
}
@media (max-width: 560px) {
  .row {
    flex-direction: column;
    gap: 4px;
  }
  .label {
    flex-basis: auto;
  }
}
</style>
