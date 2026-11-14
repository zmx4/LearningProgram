<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ file: File | null }>()
const emit = defineEmits<{ confirm: [blob: Blob] }>()
const visible = defineModel<boolean>({ default: false })

const { t } = useI18n()
const cropImg = ref<HTMLImageElement | null>(null)
const srcUrl = ref('')
const cropping = ref(false)
// 裁剪输出统一为 512×512 PNG，后端按 Content-Type 与魔数校验
const OUTPUT_SIZE = 512

let cropper: Cropper | null = null

watch(visible, (open) => {
  if (open) {
    if (!props.file) {
      visible.value = false
      return
    }
    srcUrl.value = URL.createObjectURL(props.file)
    // 等待 :key=srcUrl 的 <img> 渲染完成后再初始化，保证每次打开都是全新实例
    nextTick(() => {
      if (!cropImg.value) return
      cropper = new Cropper(cropImg.value, {
        aspectRatio: 1,
        viewMode: 1,
        dragMode: 'move',
        autoCropArea: 1,
        background: false,
        highlight: false,
        toggleDragModeOnDblclick: false,
      })
    })
  } else {
    destroyCropper()
  }
})

function destroyCropper() {
  cropper?.destroy()
  cropper = null
  if (srcUrl.value) {
    URL.revokeObjectURL(srcUrl.value)
    srcUrl.value = ''
  }
}

function confirmCrop() {
  if (!cropper || cropping.value) return
  const canvas = cropper.getCroppedCanvas({
    width: OUTPUT_SIZE,
    height: OUTPUT_SIZE,
    imageSmoothingQuality: 'high',
  })
  if (!canvas) return
  cropping.value = true
  canvas.toBlob((blob) => {
    cropping.value = false
    if (!blob) return
    emit('confirm', blob)
    visible.value = false
  }, 'image/png')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('profile.avatarCropTitle')"
    width="480px"
    align-center
    :close-on-click-modal="false"
  >
    <p class="crop-hint">{{ t('profile.avatarCropHint') }}</p>
    <div class="crop-area">
      <img v-if="srcUrl" ref="cropImg" :key="srcUrl" :src="srcUrl" :alt="t('profile.avatarAlt')" />
    </div>
    <template #footer>
      <el-button @click="visible = false">{{ t('profile.avatarCropCancel') }}</el-button>
      <el-button type="primary" :loading="cropping" @click="confirmCrop">
        {{ t('profile.avatarCropConfirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.crop-hint {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.crop-area {
  width: 100%;
  height: 340px;
  overflow: hidden;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}
.crop-area img {
  display: block;
  max-width: 100%;
}
/* 裁剪框呈现为圆形，与页面上圆形头像的最终展示一致 */
.crop-area :deep(.cropper-view-box),
.crop-area :deep(.cropper-face) {
  border-radius: 50%;
}
</style>
