<template>
    <div>
        <div style="margin: 30px 20px">
            <el-steps :active="active" finish-status="success" align-center>
                <el-step :title="$t('welcome.verifyEmail')" />
                <el-step :title="$t('welcome.resetPasswordStep')" />
            </el-steps>
        </div>
        <transition name="el-fade-in-linear" mode="out-in">
            <div style="text-align: center;margin: 0 20px;height: 100%" v-if="active === 0">
                <div style="margin-top: 80px">
                    <div style="font-size: 25px;font-weight: bold">{{ $t('welcome.resetPassword') }}</div>
                    <div style="font-size: 14px;color: grey">{{ $t('welcome.resetDescription') }}</div>
                </div>
                <div style="margin-top: 50px">
                    <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef">
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
                <div style="margin-top: 70px">
                    <el-button @click="confirmReset()" style="width: 270px;" type="danger" plain>{{ $t('welcome.startReset') }}</el-button>
                </div>
            </div>
        </transition>
        <transition name="el-fade-in-linear" mode="out-in">
            <div style="text-align: center;margin: 0 20px;height: 100%" v-if="active === 1">
                <div style="margin-top: 80px">
                    <div style="font-size: 25px;font-weight: bold">{{ $t('welcome.resetPassword') }}</div>
                    <div style="font-size: 14px;color: grey">{{ $t('welcome.resetDescription2') }}</div>
                </div>
                <div style="margin-top: 50px">
                    <el-form :model="form" :rules="rules" @validate="onValidate" ref="formRef">
                        <el-form-item prop="password">
                            <el-input v-model="form.password" :maxlength="policy.maxLength" type="password" :placeholder="$t('welcome.newPassword')">
                                <template #prefix>
                                    <el-icon><Lock /></el-icon>
                                </template>
                            </el-input>
                            <PasswordPolicyHint :requirements="policy.requirements" />
                        </el-form-item>
                        <el-form-item prop="password_repeat">
                            <el-input v-model="form.password_repeat" :maxlength="policy.maxLength" type="password" :placeholder="$t('welcome.repeatNewPassword')">
                                <template #prefix>
                                    <el-icon><Lock /></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                    </el-form>
                </div>
                <div style="margin-top: 70px">
                    <el-button @click="doReset()" style="width: 270px;" type="danger" plain>{{ $t('welcome.resetNow') }}</el-button>
                </div>
            </div>
        </transition>
    </div>
</template>

<script setup>
import {onMounted, reactive, ref} from "vue";
import {useI18n} from 'vue-i18n'
import {EditPen, Lock, Message} from "@element-plus/icons-vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";
import router from "@/router";
import PasswordPolicyHint from "@/components/PasswordPolicyHint.vue";
import {fallbackPasswordPolicy, firstUnmetRequirement, loadPasswordPolicy} from "@/utils/passwordPolicy";

const { t } = useI18n()
const active = ref(0)

// 密码规则来自后端策略
const policy = ref(fallbackPasswordPolicy)
onMounted(async () => {
    policy.value = await loadPasswordPolicy()
})

const form = reactive({
    email: '',
    code: '',
    password: '',
    password_repeat: '',
})

const validatePasswordStrength = (rule, value, callback) => {
    if (!value) {
        callback(new Error(t('validation.requiredPassword')))
        return
    }
    const unmet = firstUnmetRequirement(value, policy.value, { email: form.email })
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
    email: [
        { required: true, message: t('validation.requiredEmail'), trigger: 'blur' },
        {type: 'email', message: t('validation.validEmail'), trigger: ['blur', 'change']}
    ],
    code: [
        { required: true, message: t('validation.requiredCode'), trigger: 'blur' },
    ],
    password: [
        { validator: validatePasswordStrength, trigger: ['blur', 'change'] }
    ],
    password_repeat: [
        { validator: validatePassword, trigger: ['blur', 'change'] },
    ],
}

const formRef = ref()
const isEmailValid = ref(false)
const coldTime = ref(0)

const onValidate = (prop, isValid) => {
    if(prop === 'email')
        isEmailValid.value = isValid
}

const validateEmail = () => {
    coldTime.value = 60
    get(`/api/auth/ask-code?email=${form.email}&type=reset`, () => {
        ElMessage.success(t('welcome.codeSent', { email: form.email }))
        const handle = setInterval(() => {
          coldTime.value--
          if(coldTime.value === 0) {
            clearInterval(handle)
          }
        }, 1000)
    }, (message) => {
        ElMessage.warning(message)
        coldTime.value = 0
    })
}

const confirmReset = () => {
    formRef.value.validate((isValid) => {
        if(isValid) {
            post('/api/auth/reset-confirm', {
                email: form.email,
                code: form.code
            }, () => active.value++)
        }
    })
}

const doReset = () => {
    formRef.value.validate((isValid) => {
        if(isValid) {
            post('/api/auth/reset-password', {
                email: form.email,
                code: form.code,
                password: form.password
            }, () => {
                ElMessage.success(t('welcome.resetSuccess'))
                router.push('/')
            })
        }
    })
}

</script>

<style scoped>

</style>
