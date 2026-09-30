<template>
  <section class="panel">
    <h2 class="title">◎ 파일 업로드</h2>
    <p class="desc">
      차단 정책은 서버에서 검사합니다. 파일은 1개씩, 최대 {{ maxSizeMb }}MB까지 업로드할 수 있습니다.
    </p>

    <form class="upload-form" @submit.prevent="upload">
      <input
        ref="fileInput"
        type="file"
        class="file-input"
        :disabled="uploading"
        aria-label="업로드할 파일 선택"
        @change="select"
      >
      <button type="submit" class="btn primary" :disabled="!file || uploading">
        {{ uploading ? `업로드 중... ${progress}%` : '업로드' }}
      </button>
    </form>

    <div v-if="file" class="selected">
      {{ file.name }} <span class="muted">({{ formatSize(file.size) }})</span>
    </div>
    <div v-if="uploading" class="progress" role="progressbar" :aria-valuenow="progress" aria-valuemin="0" aria-valuemax="100">
      <div class="progress-bar" :style="{ width: progress + '%' }"></div>
    </div>

    <p class="message" :class="message.type" aria-live="polite">{{ message.text }}</p>

    <div v-if="history.length" class="history">
      <h3 class="history-title">이번 접속에서 업로드한 결과</h3>
      <ul>
        <li v-for="item in history" :key="item.key" :class="item.type">
          <span class="mark">{{ item.type === 'success' ? '✔' : '✖' }}</span>
          <span class="name">{{ item.name }}</span>
          <span class="muted">{{ item.text }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script>
import { uploadFile, errorMessage } from '../api'

// 서버 설정(spring.servlet.multipart.max-file-size)과 같은 값. 큰 파일을 보내기 전에 안내하기 위함 (최종 검사는 서버)
const MAX_SIZE_MB = 10

export default {
  name: 'FileUpload',
  data() {
    return {
      file: null,
      uploading: false,
      progress: 0,
      message: { type: '', text: '' },
      history: [],
      maxSizeMb: MAX_SIZE_MB
    }
  },
  methods: {
    select(event) {
      this.file = event.target.files[0] || null
      this.message = { type: '', text: '' }
      if (this.file && this.file.size > MAX_SIZE_MB * 1024 * 1024) {
        this.message = { type: 'error', text: `파일은 최대 ${MAX_SIZE_MB}MB까지 업로드할 수 있습니다.` }
        this.resetInput()
      }
    },

    async upload() {
      if (!this.file) {
        return
      }
      const file = this.file
      this.uploading = true
      this.progress = 0
      try {
        const saved = await uploadFile(file, p => { this.progress = p })
        this.record('success', saved.originalName, `업로드 완료 (${this.formatSize(saved.size)})`)
      } catch (e) {
        this.record('error', file.name, errorMessage(e))
      } finally {
        this.uploading = false
        this.resetInput()
      }
    },

    record(type, name, text) {
      this.message = { type, text: type === 'success' ? `${name} ${text}` : text }
      this.history.unshift({ key: Date.now() + Math.random(), type, name, text })
    },

    resetInput() {
      this.file = null
      this.$refs.fileInput.value = ''
    },

    formatSize(bytes) {
      if (bytes < 1024) return `${bytes}B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
      return `${(bytes / 1024 / 1024).toFixed(1)}MB`
    }
  }
}
</script>

<style scoped>
.upload-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.file-input {
  flex: 1;
  min-width: 0;
}
.selected {
  margin-top: 8px;
}
.muted {
  color: var(--muted);
  font-size: 13px;
}
.progress {
  margin-top: 8px;
  height: 6px;
  border-radius: 3px;
  background: var(--border);
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: var(--primary);
  transition: width 0.2s;
}
.history {
  margin-top: 16px;
  border-top: 1px solid var(--border);
  padding-top: 12px;
}
.history-title {
  margin: 0 0 8px;
  font-size: 14px;
}
.history ul {
  margin: 0;
  padding: 0;
  list-style: none;
}
.history li {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 0;
  word-break: break-all;
}
.history li.success .mark {
  color: var(--success);
}
.history li.error .mark {
  color: var(--danger);
}
.name {
  font-weight: 600;
}
</style>
