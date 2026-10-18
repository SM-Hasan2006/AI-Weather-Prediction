/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        navy: { 950: '#091125', 900: '#0d1a33', 800: '#13244a', 700: '#1c3262', 600: '#2a457f' },
        sky: { 300: '#7fdcf5', 400: '#38c9ee', 500: '#1fb0d8' },
        ink: { DEFAULT: '#eaf1ff', soft: '#b4c3e0', mute: '#8397bd' },
        risk: { low: '#34d399', moderate: '#facc15', high: '#fb923c', veryHigh: '#f87171', none: '#94a3b8' },
      },
      fontFamily: {
        display: ['Sora', 'system-ui', 'sans-serif'],
        sans: ['"Hanken Grotesk"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
