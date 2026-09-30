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
    <el-divider>
      <span style="color: grey;font-size: 13px">{{ $t('welcome.noAccount') }}</span>
    </el-divider>
    <div>
      <el-button style="width: 270px" @click="router.push('/register')" type="warning" plain>{{ $t('welcome.register') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import {User, Lock} from '@element-plus/icons-vue'
import router from "@/router";
import {reactive, ref} from "vue";
import {useI18n} from 'vue-i18n'
import {login} from '@/net'

const { t } = useI18n()
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: t('validation.requiredUsername') }
  ],
  password: [
    { required: true, message: t('validation.requiredPassword')}
  ]
}

function userLogin() {
  formRef.value.validate((isValid) => {
    if(isValid) {
      login(form.username, form.password, form.remember, () => router.push("/index"))
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