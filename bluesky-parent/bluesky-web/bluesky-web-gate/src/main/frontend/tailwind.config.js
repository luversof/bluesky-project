/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["../resources/templates/**/*.{html,js,mustache}"], // it will be explained later
  theme: {
    extend: {},
  },
  plugins: [require("@tailwindcss/typography"), require("daisyui")]
}