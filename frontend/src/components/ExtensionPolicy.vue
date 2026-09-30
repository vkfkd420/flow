<template>
  <section class="panel">
    <h2 class="title">◎ 파일 확장자 차단</h2>
    <p class="desc">파일확장자에 따라 특정 형식의 파일을 첨부하거나 전송하지 못하도록 제한</p>

    <div v-if="loading" class="state">불러오는 중...</div>
    <div v-else-if="loadError" class="state error">
      {{ loadError }}
      <button type="button" class="btn" @click="load">다시 시도</button>
    </div>

    <template v-else>
      <div class="row">
        <div class="label">고정 확장자</div>
        <div class="field fixed-list">
          <label v-for="item in fixed" :key="item.extension" class="check">
            <input
              type="checkbox"
              :checked="item.blocked"
              :disabled="savingFixed[item.extension]"
              @change="toggleFixed(item, $event.target.checked)"
            >
            {{ item.extension }}
          </label>
        </div>
      </div>

      <div class="row">
        <div class="label">커스텀 확장자</div>
        <div class="field">
          <form class="add-form" @submit.prevent="add">
            <input
              v-model="input"
              type="text"
              class="text-input"
              placeholder="확장자 입력"
              :maxlength="maxLength"
              :disabled="adding || isFull"
              aria-label="커스텀 확장자 입력"
            >
            <button type="submit" class="btn primary" :disabled="adding || isFull || !input.trim()">
              {{ adding ? '추가 중...' : '+추가' }}
            </button>
          </form>

          <div class="custom-box">
            <div class="count" :class="{ full: isFull }">{{ custom.length }}/{{ customLimit }}</div>
            <div v-if="custom.length === 0" class="empty">추가된 커스텀 확장자가 없습니다.</div>
            <span v-for="item in custom" :key="item.id" class="chip">
              {{ item.extension }}
              <button
                type="button"
                class="chip-remove"
                :disabled="deleting[item.id]"
                :aria-label="item.extension + ' 삭제'"
                @click="remove(item)"
              >X</button>
            </span>
          </div>
        </div>
      </div>

      <p class="message" :class="message.type" aria-live="polite">{{ message.text }}</p>
    </template>
  </section>
</template>

<script>
import { getPolicy, updateFixed, addCustom, deleteCustom, errorMessage, errorCode } from '../api'
import { normalizeExtension, EXTENSION_MAX_LENGTH } from '../utils/extension'

export default {
  name: 'ExtensionPolicy',
  data() {
    return {
      loading: true,
      loadError: '',
      fixed: [],
      custom: [],
      customLimit: 200,
      input: '',
      adding: false,
      savingFixed: {},
      deleting: {},
      message: { type: '', text: '' },
      maxLength: EXTENSION_MAX_LENGTH
    }
  },
  computed: {
    isFull() {
      return this.custom.length >= this.customLimit
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      this.loadError = ''
      try {
        const policy = await getPolicy()
        this.fixed = policy.fixed
        this.custom = policy.custom
        this.customLimit = policy.customLimit
      } catch (e) {
        this.loadError = errorMessage(e)
      } finally {
        this.loading = false
      }
    },

    async toggleFixed(item, blocked) {
      // 체크박스는 이미 바뀌어 있으므로 데이터도 먼저 맞춘다 (그래야 실패 시 되돌리는 값 변경이 화면에 반영됨)
      item.blocked = blocked
      this.$set(this.savingFixed, item.extension, true)
      try {
        const saved = await updateFixed(item.extension, blocked)
        item.blocked = saved.blocked
        this.notify('success', `'${item.extension}' ${saved.blocked ? '차단' : '차단 해제'}를 저장했습니다.`)
      } catch (e) {
        // 저장에 실패하면 체크 상태를 DB 값(변경 전)으로 되돌린다
        item.blocked = !blocked
        this.notify('error', errorMessage(e))
      } finally {
        this.$delete(this.savingFixed, item.extension)
      }
    },

    async add() {
      const extension = normalizeExtension(this.input)
      const clientError = this.validate(extension)
      if (clientError) {
        this.notify('error', clientError)
        return
      }
      this.adding = true
      try {
        const saved = await addCustom(extension)
        this.custom.push(saved)
        this.input = ''
        this.notify('success', `'${saved.extension}'를 추가했습니다.`)
      } catch (e) {
        this.notify('error', errorMessage(e))
      } finally {
        this.adding = false
      }
    },

    validate(extension) {
      if (!extension) {
        return `확장자는 영문과 숫자로 1~${EXTENSION_MAX_LENGTH}자까지 입력할 수 있습니다.`
      }
      if (this.fixed.some(f => f.extension === extension)) {
        return `'${extension}'는 고정 확장자입니다. 고정 확장자 영역에서 체크해 주세요.`
      }
      if (this.custom.some(c => c.extension === extension)) {
        return `'${extension}'는 이미 추가된 확장자입니다.`
      }
      if (this.isFull) {
        return `커스텀 확장자는 최대 ${this.customLimit}개까지 추가할 수 있습니다.`
      }
      return null
    },

    async remove(item) {
      this.$set(this.deleting, item.id, true)
      try {
        await deleteCustom(item.id)
        this.removeFromList(item)
        this.notify('success', `'${item.extension}'를 삭제했습니다.`)
      } catch (e) {
        if (errorCode(e) === 'CUSTOM_EXTENSION_NOT_FOUND') {
          // 다른 곳에서 이미 삭제된 경우: 화면도 DB 상태에 맞춘다
          this.removeFromList(item)
        }
        this.notify('error', errorMessage(e))
      } finally {
        this.$delete(this.deleting, item.id)
      }
    },

    removeFromList(item) {
      this.custom = this.custom.filter(c => c.id !== item.id)
    },

    notify(type, text) {
      this.message = { type, text }
    }
  }
}
</script>

<style scoped>
.fixed-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}
.check {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.add-form {
  display: flex;
  gap: 8px;
}
.text-input {
  flex: 1;
  max-width: 280px;
}
.custom-box {
  margin-top: 10px;
  min-height: 90px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
}
.count {
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 13px;
}
.count.full {
  color: var(--danger);
  font-weight: 600;
}
.empty {
  color: var(--muted);
  font-size: 13px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 6px 6px 0;
  padding: 3px 4px 3px 10px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--chip);
}
.chip-remove {
  border: none;
  background: none;
  color: var(--muted);
  cursor: pointer;
  font-weight: 600;
}
.chip-remove:hover:not(:disabled) {
  color: var(--danger);
}
</style>
