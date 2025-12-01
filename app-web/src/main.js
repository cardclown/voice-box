import './assets/main.css'
import './styles/variables.css'
import './styles/themes.css'
import './styles/responsive.css'
import './styles/markdown.css'
import 'highlight.js/styles/github-dark.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.mount('#app')
