// 서버 ExtensionRule과 같은 규칙 (입력 즉시 안내용, 최종 검증은 서버)
// 앞뒤 공백 제거 → 앞의 '.' 하나 제거 → 소문자 변환 → 영문 소문자/숫자 1~20자
export const EXTENSION_MAX_LENGTH = 20

const FORMAT = /^[a-z0-9]{1,20}$/

export function normalizeExtension(input) {
  let value = (input || '').trim()
  if (value.startsWith('.')) {
    value = value.substring(1)
  }
  value = value.toLowerCase()
  return FORMAT.test(value) ? value : null
}
