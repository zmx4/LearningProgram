<template>
  <div style="text-align: center;margin: 0 20px">
    <div style="margin-top: 100px">
      <div style="font-size: 25px;font-weight: bold">{{ $t('welcome.registerTitle') }}</div>
      <div style="font-size: 14px;color: grey">{{ $t('welcome.registerDescription') }}</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef" @submit.prevent="register">
        <el-form-item prop="username">
          <el-input v-model="form.username" :maxlength="8" type="text" :aria-label="$t('welcome.username')" :placeholder="$t('welcome.username')">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" @input="revalidatePasswordRepeat" :maxlength="policy.maxLength" type="password" show-password :aria-label="$t('welcome.password')" :placeholder="$t('welcome.password')">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
          <PasswordPolicyHint :requirements="policy.requirements" :value="form.password" :username="form.username" :email="form.email" />
        </el-form-item>
        <el-form-item prop="password_repeat">
          <el-input v-model="form.password_repeat" :maxlength="policy.maxLength" type="password" show-password :aria-label="$t('welcome.repeatPassword')" :placeholder="$t('welcome.repeatPassword')">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" type="email" :aria-label="$t('welcome.email')" :placeholder="$t('welcome.email')">
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code">
          <el-row class="code-row" :gutter="10">
            <el-col class="code-input" :span="16">
              <el-input v-model="form.code" :maxlength="6" type="text" inputmode="numeric" :aria-label="$t('welcome.verificationCode')" :placeholder="$t('welcome.verificationCode')">
                <template #prefix>
                  <el-icon><EditPen /></el-icon>
                </template>
              </el-input>
            </el-col>
            <el-col class="code-action" :span="8">
              <el-button type="primary" @click="validateEmail"
                         :disabled="!isEmailValid || coldTime > 0 || codeLoading" :loading="codeLoading">
                {{coldTime > 0 ? $t('welcome.waitSeconds', { seconds: coldTime }) : $t('welcome.getCode')}}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-top: 80px">
      <el-button style="width: 270px" type="primary" @click="register" plain :loading="registerLoading">{{ $t('welcome.registerNow') }}</el-button>
    </div>
    <div style="margin-top: 20px">
      <span style="font-size: 14px;line-height: 15px;color: grey">{{ $t('welcome.haveAccount') }} </span>
      <el-link type="primary" style="translate: 0 -2px" @click="router.push('/')">{{ $t('welcome.loginNow') }}</el-link>
    </div>
    <div style="margin-top: 30px;text-align: center">
      <UserAgreementCheckbox v-model="form.agree" style="display: inline-block"/>
    </div>
  </div>
</template>

<script setup>
import {EditPen, Lock, Message, User} from "@element-plus/icons-vue";
import {onMounted, reactive, ref} from "vue";
import {useI18n} from 'vue-i18n'
import {ElMessage} from "element-plus";
import {publicGet, publicPost} from "@/net";
import PasswordPolicyHint from "@/components/PasswordPolicyHint.vue";
import UserAgreementCheckbox from "@/components/UserAgreementCheckbox.vue";
import {fallbackPasswordPolicy, firstUnmetRequirement, loadPasswordPolicy} from "@/utils/passwordPolicy";
import {useRouter} from "vue-router";

const { t } = useI18n()
const router = useRouter()

// 密码规则来自后端策略，避免前后端各写一份
const policy = ref(fallbackPasswordPolicy)
onMounted(async () => {
  policy.value = await loadPasswordPolicy()
})
const form = reactive({
  username: '',
  password: '',
  password_repeat: '',
  email: '',
  code: '',
  agree: false
})

const validateUsername = (rule, value, callback) => {
  if (value === '') {
    callback(new Error(t('validation.requiredUsername')))
  } else if(!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)){
    callback(new Error(t('validation.usernameCharacters')))
  } else {
    callback()
  }
}

const validatePasswordStrength = (rule, value, callback) => {
  if (!value) {
    callback(new Error(t('validation.requiredPassword')))
    return
  }
  const unmet = firstUnmetRequirement(value, policy.value, { username: form.username, email: form.email })
  if (unmet) {
    callback(new Error(unmet))
    return
  }
  callback()
}

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error(t('validation.repeatPassword')))
  } else if (value !== form.password) {
    callback(new Error(t('validation.passwordMismatch')))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { validator: validateUsername, trigger: ['blur', 'change'] },
    { min: 2, max: 8, message: t('validation.usernameLength'), trigger: ['blur', 'change'] },
  ],
  password: [
    { validator: validatePasswordStrength, trigger: ['blur', 'change'] }
  ],
  password_repeat: [
    { validator: validatePassword, trigger: ['blur', 'change'] },
  ],
  email: [
    { required: true, message: t('validation.requiredEmail'), trigger: 'blur' },
    {type: 'email', message: t('validation.validEmail'), trigger: ['blur', 'change']}
  ],
  code: [
    { required: true, message: t('validation.requiredCode'), trigger: 'blur' },
  ]
}

const formRef = ref()
const isEmailValid = ref(false)
const coldTime = ref(0)
const codeLoading = ref(false)
const registerLoading = ref(false)

const onValidate = (prop, isValid) => {
  if(prop === 'email')
    isEmailValid.value = isValid
}

const revalidatePasswordRepeat = () => {
  if (form.password_repeat) formRef.value?.validateField('password_repeat')
}

const register = () => {
  if (registerLoading.value) return
  if(!form.agree) {
    ElMessage.warning(t('welcome.agreementRequired'))
    return
  }
  formRef.value.validate((isValid) => {
    if(isValid) {
      registerLoading.value = true
      publicPost('/api/auth/register', {
        username: form.username,
        password: form.password,
        email: form.email,
        code: form.code
      }, () => {
        registerLoading.value = false
        ElMessage.success(t('welcome.registerSuccess'))
        router.push("/")
      }, () => {
        registerLoading.value = false
      })
    } else {
      ElMessage.warning(t('welcome.completeRegistration'))
    }
  })
}

const validateEmail = () => {
  if (codeLoading.value || coldTime.value > 0) return
  codeLoading.value = true
  publicGet(`/api/auth/ask-code?email=${encodeURIComponent(form.email)}&type=register`, () => {
    codeLoading.value = false
    coldTime.value = 60
    ElMessage.success(t('welcome.codeSent', { email: form.email }))
    const handle = setInterval(() => {
      coldTime.value--
      if(coldTime.value === 0) {
        clearInterval(handle)
      }
    }, 1000)
  }, undefined, (message) => {
    codeLoading.value = false
    ElMessage.warning(message)
    coldTime.value = 0
  })
}
</script>

<style scoped>
.code-row {
  width: 100%;
}

.code-input {
  min-width: 0;
}

.code-action {
  display: flex;
  min-width: 0;
}

.code-action .el-button {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

</style>
