import {
  computed,
  inject,
  onUnmounted,
  provide,
  ref,
  shallowRef,
  watch,
  type Ref,
} from 'vue'

export interface ArtTableFullscreenContext {
  exitFullScreen: () => void
  isFullScreen: Ref<boolean>
  registerTarget: (el: HTMLElement | null) => void
  toggleFullScreen: () => void
}

export const ART_TABLE_FULLSCREEN_KEY = Symbol('art-table-fullscreen')

const targetStates = new WeakMap<HTMLElement, Ref<boolean>>()

function getTargetState(el: HTMLElement) {
  if (!targetStates.has(el)) {
    targetStates.set(el, ref(false))
  }
  return targetStates.get(el)!
}

function createArtTableFullscreenContext(): ArtTableFullscreenContext {
  const isFullScreen = ref(false)
  const targetRef = ref<HTMLElement | null>(null)
  let originalOverflow = ''

  const applyFullScreen = (value: boolean) => {
    const el = targetRef.value
    if (!el) return

    isFullScreen.value = value
    getTargetState(el).value = value

    if (value) {
      originalOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      el.classList.add('art-table-is-fullscreen')
      return
    }

    document.body.style.overflow = originalOverflow
    el.classList.remove('art-table-is-fullscreen')
  }

  const toggleFullScreen = () => applyFullScreen(!isFullScreen.value)

  const exitFullScreen = () => {
    if (isFullScreen.value) {
      applyFullScreen(false)
    }
  }

  const registerTarget = (el: HTMLElement | null) => {
    if (targetRef.value && targetRef.value !== el) {
      targetRef.value.classList.remove('art-table-is-fullscreen')
    }
    targetRef.value = el
    if (el) {
      isFullScreen.value = getTargetState(el).value
    }
  }

  const handleEscapeKey = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && isFullScreen.value) {
      exitFullScreen()
    }
  }

  if (typeof document !== 'undefined') {
    document.addEventListener('keydown', handleEscapeKey)
    onUnmounted(() => {
      document.removeEventListener('keydown', handleEscapeKey)
      exitFullScreen()
    })
  }

  return {
    exitFullScreen,
    isFullScreen,
    registerTarget,
    toggleFullScreen,
  }
}

export function provideArtTableFullscreen() {
  const context = createArtTableFullscreenContext()
  provide(ART_TABLE_FULLSCREEN_KEY, context)
  return context
}

export function useArtTableFullscreen() {
  return inject<ArtTableFullscreenContext | null>(ART_TABLE_FULLSCREEN_KEY, null)
}

export function resolveArtTableFullscreenTarget(
  root: HTMLElement | null | undefined,
  selector = '[data-art-table-fullscreen]',
) {
  return root?.closest(selector) as HTMLElement | null
}

export function createFallbackFullscreenController(root: HTMLElement | null | undefined) {
  const target = resolveArtTableFullscreenTarget(root)
  if (!target) {
    return null
  }

  const isFullScreen = getTargetState(target)
  let originalOverflow = ''

  const applyFullScreen = (value: boolean) => {
    isFullScreen.value = value

    if (value) {
      originalOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      target.classList.add('art-table-is-fullscreen')
      return
    }

    document.body.style.overflow = originalOverflow
    target.classList.remove('art-table-is-fullscreen')
  }

  const toggleFullScreen = () => applyFullScreen(!isFullScreen.value)

  const exitFullScreen = () => {
    if (isFullScreen.value) {
      applyFullScreen(false)
    }
  }

  const handleEscapeKey = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && isFullScreen.value) {
      exitFullScreen()
    }
  }

  document.addEventListener('keydown', handleEscapeKey)
  onUnmounted(() => {
    document.removeEventListener('keydown', handleEscapeKey)
    exitFullScreen()
  })

  return {
    exitFullScreen,
    isFullScreen,
    toggleFullScreen,
  }
}

export function useArtTableFullscreenState(root: Ref<HTMLElement | null | undefined>) {
  const injected = useArtTableFullscreen()
  const fallback = shallowRef<ReturnType<typeof createFallbackFullscreenController>>(null)

  watch(
    () => root.value,
    (el) => {
      if (!injected) {
        fallback.value = createFallbackFullscreenController(el)
      }
    },
    { immediate: true },
  )

  const isFullScreen = computed(() => {
    if (injected) return injected.isFullScreen.value
    return fallback.value?.isFullScreen.value ?? false
  })

  const toggleFullScreen = () => {
    if (injected) {
      injected.toggleFullScreen()
      return
    }
    fallback.value?.toggleFullScreen()
  }

  return { isFullScreen, toggleFullScreen }
}

export function getArtTableFullscreenState(root: HTMLElement | null | undefined) {
  const target = resolveArtTableFullscreenTarget(root)
  if (!target) {
    return ref(false)
  }
  return getTargetState(target)
}
