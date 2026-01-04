<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import { buildDraftFromReservation, createEmptyDraft, loadDraft, saveDraft, type LiveCreateDraft } from '../../composables/useLiveCreateDraft'

const router = useRouter()
const route = useRoute()

const maxQuestions = 10
const draft = ref<LiveCreateDraft>(createEmptyDraft())
const error = ref('')

const reservationId = computed(() => (typeof route.query.reservationId === 'string' ? route.query.reservationId : ''))
const isEditMode = computed(() => route.query.mode === 'edit' && !!reservationId.value)

const createQuestion = () => ({
  id: `q-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  text: '',
})

const syncDraft = () => {
  saveDraft({
    ...draft.value,
    cueTitle: draft.value.cueTitle.trim(),
    cueNotes: draft.value.cueNotes.trim(),
    questions: draft.value.questions.map((q) => ({ ...q, text: q.text.trim() })),
  })
}

const restoreDraft = () => {
  const saved = loadDraft()
  if (saved && (!isEditMode.value || saved.reservationId === reservationId.value)) {
    draft.value = { ...draft.value, ...saved }
  }

  if (isEditMode.value) {
    draft.value = { ...draft.value, ...buildDraftFromReservation(reservationId.value) }
  }

  if (!draft.value.questions.length) {
    draft.value.questions = [createQuestion()]
  }

  syncDraft()
}

const addQuestion = () => {
  if (draft.value.questions.length >= maxQuestions) return
  draft.value.questions.push(createQuestion())
}

const removeQuestion = (id: string) => {
  if (draft.value.questions.length <= 1) return
  draft.value.questions = draft.value.questions.filter((item) => item.id !== id)
}

const isQuestionValid = (text: string) => {
  const trimmed = text.trim()
  return !!trimmed && trimmed.includes('?')
}

const goNext = () => {
  const hasInvalid = draft.value.questions.some((q) => !isQuestionValid(q.text))
  if (hasInvalid) {
    error.value = '모든 항목은 질문 형태( ? 포함 )로 작성해주세요.'
    return
  }
  error.value = ''
  syncDraft()
  router.push({ path: '/seller/live/create/basic', query: route.query }).catch(() => {})
}

onMounted(() => {
  restoreDraft()
})

watch(
  draft,
  () => {
    syncDraft()
  },
  { deep: true },
)
</script>

<template>
  <PageContainer>
    <PageHeader :eyebrow="isEditMode ? 'DESKIT' : 'DESKIT'" :title="isEditMode ? '예약 수정 - 큐 카드 편집' : '방송 등록 - 큐 카드 작성'" />
    <section class="create-card ds-surface">
      <div class="step-meta">
        <span class="step-label">1 / 2 단계</span>
        <button type="button" class="btn ghost" @click="router.back()">이전</button>
      </div>
      <label class="field">
        <span class="field__label">큐 카드 제목</span>
        <input v-model="draft.cueTitle" type="text" placeholder="예: 오프닝 멘트" />
      </label>
      <label class="field">
        <span class="field__label">큐 카드 메모</span>
        <textarea v-model="draft.cueNotes" rows="4" placeholder="방송 흐름을 간단히 적어주세요."></textarea>
      </label>
      <div class="section-head">
        <h3>큐 카드 질문</h3>
        <span class="count-pill">{{ draft.questions.length }}/{{ maxQuestions }}</span>
      </div>
      <div class="question-list">
        <div v-for="(item, index) in draft.questions" :key="item.id" class="question-card" :class="{ invalid: !isQuestionValid(item.text) }">
          <div class="question-head">
            <span class="question-title">질문 {{ index + 1 }}</span>
            <button type="button" class="btn ghost" :disabled="draft.questions.length <= 1" @click="removeQuestion(item.id)">
              삭제
            </button>
          </div>
          <textarea v-model="item.text" rows="3" placeholder="질문 내용을 입력하세요. ( ? 를 포함한 질문 형태만 허용됩니다 )"></textarea>
        </div>
      </div>
      <div class="question-actions">
        <button type="button" class="btn" :disabled="draft.questions.length >= maxQuestions" @click="addQuestion">
          + 질문 추가
        </button>
        <span v-if="draft.questions.length >= maxQuestions" class="hint">최대 10개까지 추가할 수 있어요.</span>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <div class="step-hint">질문형 입력을 모두 채워야 다음 단계로 이동할 수 있습니다.</div>
        <button type="button" class="btn primary" @click="goNext">다음 단계</button>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.create-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.step-label {
  font-weight: 800;
  color: var(--text-muted);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field__label {
  font-weight: 800;
  color: var(--text-strong);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.count-pill {
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  color: var(--text-strong);
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 800;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  background: var(--surface);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-card.invalid {
  border-color: #ef4444;
}

.question-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.question-title {
  font-weight: 800;
  color: var(--text-strong);
}

.question-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.error {
  margin: 0;
  color: #ef4444;
  font-weight: 800;
}

input,
textarea {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn {
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 900;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  cursor: pointer;
}

.btn.ghost {
  border-color: var(--border-color);
  color: var(--text-muted);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.step-hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}
</style>
