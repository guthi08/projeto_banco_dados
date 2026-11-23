import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
  theme: {
    extend: {
      fontFamily: {
        sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace']
      },
      colors: {
        canvas: '#f3f5f9',
        sidebar: {
          bg: '#131c2e',
          surface: '#1b273d',
          hover: '#23324d',
          active: '#1f2e4a',
          border: '#26354f',
          ink: '#f8fafc',
          muted: '#94a3b8'
        }
      }
    }
  }
}
