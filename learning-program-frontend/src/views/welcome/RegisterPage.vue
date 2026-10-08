<template>
  <div style="text-align: center;margin: 0 20px">
    <div style="margin-top: 100px">
      <div style="font-size: 25px;font-weight: bold">{{ $t('welcome.registerTitle') }}</div>
      <div style="font-size: 14px;color: grey">{{ $t('welcome.registerDescription') }}</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" :maxlength="8" type="text" :placeholder="$t('welcome.username')">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" :maxlength="policy.maxLength" type="password" :placeholder="$t('welcome.password')">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
          <PasswordPolicyHint :requirements="policy.requirements" :value="form.password" :username="form.username" :email="form.email" />
        </el-form-item>
        <el-form-item prop="password_repeat">
          <el-input v-model="form.password_repeat" :maxlength="policy.maxLength" type="password" :placeholder="$t('welcome.repeatPassword')">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" type="email" :placeholder="$t('welcome.email')">
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code">
          <el-row :gutter="10" style="width: 100%">
            <el-col :span="17">
              <el-input v-model="form.code" :maxlength="6" type="text" :placeholder="$t('welcome.verificationCode')">
                <template #prefix>
                  <el-icon><EditPen /></el-icon>
                </template>
              </el-input>
            </el-col>
            <el-col :span="5">
              <el-button type="success" @click="validateEmail"
                         :disabled="!isEmailValid || coldTime > 0">
                {{coldTime > 0 ? $t('welcome.waitSeconds', { seconds: coldTime }) : $t('welcome.getCode')}}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-top: 80px">
      <el-button style="width: 270px" type="warning" @click="register" plain>{{ $t('welcome.registerNow') }}</el-button>
    </div>
    <div style="margin-top: 20px">
      <span style="font-size: 14px;line-height: 15px;color: grey">{{ $t('welcome.haveAccount') }} </span>
      <el-link type="primary" style="translate: 0 -2px" @click="router.push('/')">{{ $t('welcome.loginNow') }}</el-link>
    </div>
  </div>
</template>

<script setup>
import {EditPen, Lock, Message, User} from "@element-plus/icons-vue";
import router from "@/router";
import {onMounted, reactive, ref} from "vue";
import {useI18n} from 'vue-i18n'
import {ElMessage} from "element-plus";
import {publicGet, publicPost} from "@/net";
import PasswordPolicyHint from "@/components/PasswordPolicyHint.vue";
import {fallbackPasswordPolicy, firstUnmetRequirement, loadPasswordPolicy} from "@/utils/passwordPolicy";

const { t } = useI18n()

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
  code: ''
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

const onValidate = (prop, isValid) => {
  if(prop === 'email')
    isEmailValid.value = isValid
}

const register = () => {
  formRef.value.validate((isValid) => {
    if(isValid) {
      publicPost('/api/auth/register', {
        username: form.username,
        password: form.password,
        email: form.email,
        code: form.code
      }, () => {
        ElMessage.success(t('welcome.registerSuccess'))
        router.push("/")
      })
    } else {
      ElMessage.warning(t('welcome.completeRegistration'))
    }
  })
}

const validateEmail = () => {
  coldTime.value = 60
  publicGet(`/api/auth/ask-code?email=${form.email}&type=register`, () => {
    ElMessage.success(t('welcome.codeSent', { email: form.email }))
    const handle = setInterval(() => {
      coldTime.value--
      if(coldTime.value === 0) {
        clearInterval(handle)
      }
    }, 1000)
  }, undefined, (message) => {
    ElMessage.warning(message)
    coldTime.value = 0
  })
}
</script>

<style scoped>

</style>
