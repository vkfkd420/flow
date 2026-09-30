import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export function getPolicy() {
  return http.get('/extensions').then(res => res.data)
}

export function updateFixed(extension, blocked) {
  return http.patch(`/extensions/fixed/${encodeURIComponent(extension)}`, { blocked }).then(res => res.data)
}

export function addCustom(extension) {
  return http.post('/extensions/custom', { extension }).then(res => res.data)
}

export function deleteCustom(id) {
  return http.delete(`/extensions/custom/${id}`)
}

export function uploadFile(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/files', form, {
    onUploadProgress: e => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    }
  }).then(res => res.data)
}

/** 서버의 { code, message } 에러를 화면에 보여줄 문장으로 바꾼다 */
export function errorMessage(error) {
  const data = error && error.response && error.response.data
  if (data && data.message) {
    return data.message
  }
  if (error && error.response) {
    // 백엔드 에러는 항상 { code, message } 이므로, 그 형식이 아니면 프록시·게이트웨이 단계에서 실패한 것
    return `서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요. (HTTP ${error.response.status})`
  }
  return '서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.'
}

export function errorCode(error) {
  const data = error && error.response && error.response.data
  return data ? data.code : null
}
