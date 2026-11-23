export default defineNuxtConfig({
  compatibilityDate: '2025-02-01',
  telemetry: false,
  devtools: { enabled: true },

  modules: [
    '@nuxt/ui',
    '@pinia/nuxt'
  ],

  colorMode: {
    preference: 'light',
    fallback: 'light'
  },

  css: ['~/assets/css/tailwind.css'],

  app: {
    head: {
      htmlAttrs: {
        lang: 'pt-BR'
      },
      title: 'DRONE-MONITOR — Gestão e Monitoramento de Voos com Drones',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        {
          name: 'description',
          content: 'Plataforma de gestão, agendamento e auditoria de operações de voo com drones em conformidade ANAC/DECEA.'
        }
      ],
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&family=IBM+Plex+Sans:wght@400;500;600;700&display=swap'
        }
      ]
    }
  },

  runtimeConfig: {
    public: {
      apiBaseUrl: 'http://localhost:8080/api/v1'
    }
  },

  typescript: {
    strict: true
  }
})
