import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

import {
  Alert,
  Avatar,
  Badge,
  Breadcrumb,
  BreadcrumbItem,
  Button,
  Checkbox,
  Col,
  Dialog,
  Dropdown,
  DropdownItem,
  DropdownMenu,
  Empty,
  Form,
  FormItem,
  Input,
  InputNumber,
  Loading,
  Menu,
  MenuItem,
  Message,
  MessageBox,
  Option,
  Pagination,
  Popconfirm,
  Popover,
  Row,
  Select,
  Switch,
  Table,
  TableColumn,
  TabPane,
  Tabs,
  Tag,
  Tooltip
} from 'element-ui'

import '@/assets/styles/variables.css'
import '@/assets/styles/global.css'

const elementComponents = [
  Alert,
  Avatar,
  Badge,
  Breadcrumb,
  BreadcrumbItem,
  Button,
  Checkbox,
  Col,
  Dialog,
  Dropdown,
  DropdownItem,
  DropdownMenu,
  Empty,
  Form,
  FormItem,
  Input,
  InputNumber,
  Menu,
  MenuItem,
  Option,
  Pagination,
  Popconfirm,
  Popover,
  Row,
  Select,
  Switch,
  Table,
  TableColumn,
  TabPane,
  Tabs,
  Tag,
  Tooltip
]
elementComponents.forEach(component => Vue.use(component))
Vue.use(Loading.directive)
Vue.prototype.$loading = Loading.service
Vue.prototype.$message = Message
Vue.prototype.$msgbox = MessageBox
Vue.prototype.$alert = MessageBox.alert
Vue.prototype.$confirm = MessageBox.confirm
Vue.prototype.$prompt = MessageBox.prompt

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
