<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { describeRequirements, type PasswordRequirement } from '@/utils/passwordPolicy'

const props = defineProps<{
  requirements: PasswordRequirement[]
  value?: string
  username?: string
  email?: string
}>()

const statuses = computed(() =>
  describeRequirements(props.requirements, props.value ?? '', {
    username: props.username,
    email: props.email,
  }),
)
</script>

<template>
  <ul v-if="statuses.length" class="password-policy-hint">
    <li
      v-for="item in statuses"
      :key="item.code"
      class="hint-item"
      :class="value ? (item.satisfied ? 'is-met' : 'is-unmet') : 'is-idle'"
    >
      <el-icon v-if="value" class="hint-icon">
        <CircleCheckFilled v-if="item.satisfied" />
        <CircleCloseFilled v-else />
      </el-icon>
      <span>{{ item.message }}</span>
    </li>
  </ul>
</template>

<style scoped>
.password-policy-hint {
  margin: 6px 0 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  line-height: 1.75;
  text-align: left;
}
.hint-item {
  display: flex;
  align-items: center;
  gap: 5px;
  transition: color .2s;
}
.hint-icon {
  font-size: 13px;
  flex: none;
}
.is-idle { color: var(--el-text-color-placeholder); }
.is-met { color: var(--el-color-success); }
.is-unmet { color: var(--el-text-color-secondary); }
</style>
