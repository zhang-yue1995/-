<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-shape bg-shape--1"></div>
      <div class="bg-shape bg-shape--2"></div>
      <div class="bg-shape bg-shape--3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- Logo区域 -->
      <div class="login-logo">
        <div class="logo-icon">鑫</div>
        <h1 class="logo-title">鑫速录</h1>
        <p class="logo-subtitle">企业财务报表自动化填报与智能分析平台</p>
      </div>

      <!-- 登录表单 -->
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            size="large"
            clearable
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            size="large"
            show-password
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
            <a href="javascript:;" class="forgot-link">忘记密码？</a>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            :loading="loading"
            type="primary"
            size="large"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

    </div>

    <!-- 底部版权信息 -->
    <footer class="login-footer">
      <p>© 2024 鑫速录 · 企业财务报表自动化填报与智能分析平台</p>
      <p>版本 V1.0.0</p>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'Login',

  data() {
    return {
      loginForm: {
        username: '',
        password: '',
        remember: false
      },
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },

  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (!valid) return

        this.loading = true
        try {
          await this.$store.dispatch('user/login', this.loginForm)

          this.$message.success('登录成功')

          // 跳转到目标页面或首页
          const redirect = this.$route.query.redirect || '/dashboard'
          this.$router.push(redirect)
        } catch (error) {
          console.error('Login error:', error)
          this.$message.error(error.message || '登录失败，请检查用户名和密码')
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0e8f78 0%, #1dc7a3 50%, #35d0b0 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.login-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  background-color: white;
}

.bg-shape--1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -200px;
  animation: float 15s ease-in-out infinite;
}

.bg-shape--2 {
  width: 400px;
  height: 400px;
  bottom: -150px;
  left: -100px;
  animation: float 12s ease-in-out infinite reverse;
}

.bg-shape--3 {
  width: 300px;
  height: 300px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 10s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-40px);
  }
}

/* 登录卡片 */
.login-card {
  width: 420px;
  background-color: #ffffff;
  border-radius: 14px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 1;
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Logo区域 */
.login-logo {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #35d0b0 0%, #0e8f78 100%);
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(14, 143, 120, 0.3);
}

.logo-title {
  font-size: 28px;
  font-weight: 700;
  color: #10212b;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.logo-subtitle {
  font-size: 13px;
  color: #6c7d89;
  line-height: 1.5;
}

/* 登录表单 */
.login-form >>> .el-input__inner {
  height: 46px;
  border-radius: 8px;
  font-size: 14px;
  border: 1px solid #dce6eb;
  transition: all 0.3s;
}

.login-form >>> .el-input__inner:focus {
  border-color: #0e8f78;
  box-shadow: 0 0 0 3px rgba(14, 143, 120, 0.1);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.forgot-link {
  color: #0e8f78;
  text-decoration: none;
  font-size: 13px;
  transition: color 0.3s;
}

.forgot-link:hover {
  color: #1dc7a3;
  text-decoration: underline;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 999px;
  background: linear-gradient(90deg, #0e8f78 0%, #1dc7a3 100%);
  border: none;
  letter-spacing: 2px;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(14, 143, 120, 0.4);
}

.login-button.is-disabled:hover {
  transform: none;
  box-shadow: none;
}

/* 底部版权 */
.login-footer {
  position: fixed;
  bottom: 24px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 1;
}

.login-footer p {
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  margin: 4px 0;
}
</style>
