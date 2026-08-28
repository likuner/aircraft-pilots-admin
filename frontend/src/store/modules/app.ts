export type ThemeName = 'dark' | 'light'

export interface AppState {
  sidebarCollapsed: boolean
  theme: ThemeName
}

const THEME_KEY = 'uav:theme'

export function getStoredTheme(): ThemeName {
  const saved = localStorage.getItem(THEME_KEY)
  return saved === 'light' || saved === 'dark' ? saved : 'dark'
}

export function storeTheme(theme: ThemeName) {
  localStorage.setItem(THEME_KEY, theme)
}

const state: AppState = {
  sidebarCollapsed: false,
  theme: getStoredTheme()
}

type CommitFn = (mutation: string, payload?: unknown) => void

const mutations = {
  TOGGLE_SIDEBAR: (state: AppState) => {
    state.sidebarCollapsed = !state.sidebarCollapsed
  },
  SET_THEME: (state: AppState, theme: ThemeName) => {
    state.theme = theme
  }
}

const actions = {
  toggleSidebar({ commit }: { commit: CommitFn }) {
    commit('TOGGLE_SIDEBAR')
  },
  setTheme({ commit }: { commit: CommitFn }, theme: ThemeName) {
    storeTheme(theme)
    document.documentElement.classList.toggle('dark', theme === 'dark')
    commit('SET_THEME', theme)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
