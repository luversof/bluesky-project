/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["../resources/templates/**/*.{html,js}"], // it will be explained later
  theme: {
    extend: {},
  },
  daisyui: {
    themes: ["light", "dark", "winter"],
  },
  plugins: [require("@tailwindcss/typography"), require("daisyui")]
}