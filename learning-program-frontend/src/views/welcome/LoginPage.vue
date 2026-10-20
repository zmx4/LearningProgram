<template>
  <div style="text-align: center;margin: 0 20px">
    <div style="margin-top: 150px">
      <div class="login-title">{{ $t('welcome.login') }}</div>
      <div style="font-size: 14px;color: grey">{{ $t('welcome.loginDescription') }}</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" maxlength="10" type="text" :placeholder="$t('welcome.usernameOrEmail')">
            <template #prefix>
              <el-icon>
                <User/>
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" maxlength="20" style="margin-top: 10px" :placeholder="$t('welcome.password')">
            <template #prefix>
              <el-icon>
                <Lock/>
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-row style="margin-top: 5px">
          <el-col :span="12" style="text-align: left">
            <el-form-item prop="remember">
              <el-checkbox v-model="form.remember" :label="$t('welcome.remember')"/>
            </el-form-item>
          </el-col>
          <el-col :span="12" style="text-align: right">
            <el-link @click="router.push('/forget')">{{ $t('welcome.forgotPassword') }}</el-link>
          </el-col>
        </el-row>
      </el-form>
    </div>
    <div style="margin-top: 40px">
      <el-button @click="userLogin()" style="width: 270px" type="success" plain>{{ $t('welcome.loginNow') }}</el-button>
    </div>
    <el-dialog v-model="totpVisible" title="两步验证" width="360px" :close-on-click-modal="false">
      <p>请输入验证器应用中的6位验证码。</p>
      <el-input v-model="totpCode" maxlength="6" inputmode="numeric" />
      <template #footer>
        <el-button @click="totpVisible = false">取消</el-button>
        <el-button type="primary" :loading="totpLoading" @click="submitTotp">验证并登录</el-button>
      </template>
    </el-dialog>
    <el-divider>
      <span style="color: grey;font-size: 13px">{{ $t('welcome.noAccount') }}</span>
    </el-divider>
    <div>
      <el-button style="width: 270px" @click="router.push('/register')" type="warning" plain>{{ $t('welcome.register') }}</el-button>
    </div>
    <div style="margin-top: 30px;text-align: center">
      <UserAgreementCheckbox v-model="form.agree" style="display: inline-block"/>
    </div>
  </div>
</template>

<script setup>
import {User, Lock} from '@element-plus/icons-vue'
import router from "@/router";
import {reactive, ref} from "vue";
import {useI18n} from 'vue-i18n'
import {ElMessage} from "element-plus";
import {login, verifyTotpLogin} from '@/net'
import UserAgreementCheckbox from "@/components/UserAgreementCheckbox.vue";

const { t } = useI18n()
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  remember: false,
  agree: false
})
const totpVisible = ref(false)
const totpLoading = ref(false)
const totpCode = ref('')
const totpChallenge = ref('')

const rules = {
  username: [
    { required: true, message: t('validation.requiredUsername') }
  ],
  password: [
    { required: true, message: t('validation.requiredPassword')}
  ]
}

function userLogin() {
  if(!form.agree) {
    ElMessage.warning(t('welcome.agreementRequired'))
    return
  }
  formRef.value.validate((isValid) => {
    if(isValid) {
      login(form.username, form.password, form.remember, () => router.push("/index"), (message, status, _url, data) => {
        if (status === 428 && data && typeof data === 'object' && 'challenge' in data) {
          totpChallenge.value = String(data.challenge)
          totpCode.value = ''
          totpVisible.value = true
          return
        }
        ElMessage.warning(message)
      })
    }

    function submitTotp() {
      if (!/^\d{6}$/.test(totpCode.value)) {
        ElMessage.warning('请输入6位数字验证码')
        return
      }
      totpLoading.value = true
      verifyTotpLogin(totpChallenge.value, totpCode.value, form.remember, () => {
        totpLoading.value = false
        totpVisible.value = false
        void router.push('/index')
      }, (message) => {
        totpLoading.value = false
        ElMessage.warning(message)
      })
    }
  });
}
</script>

<style scoped>
.login-title {
  color: var(--el-text-color-primary);
  font-size: 25px;
  font-weight: bold;
}

</style>