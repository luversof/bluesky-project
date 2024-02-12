/** @type {import('tailwindcss').Config} */
module.exports = {
	content: ["../resources/templates/**/*.{html,js}"], // it will be explained later
	theme: {
		extend: {},
	},
	daisyui: {
		themes: [
			"light",
			"dark",
			"cupcake",
			"bumblebee",
			"emerald",
			"corporate",
			"synthwave",
			"retro",
			"cyberpunk",
			"valentine",
			"halloween",
			"garden",
			"forest",
			"aqua",
			"lofi",
			"pastel",
			"fantasy",
			"wireframe",
			"black",
			"luxury",
			"dracula",
			"cmyk",
			"autumn",
			"business",
			"acid",
			"lemonade",
			"night",
			"coffee",
			"winter",
			"dim",
			"nord",
			"sunset",
			{
				bluesky : {
					"primary": "#ff00ff",
					"secondary": "#ffffff",
					"accent": "#ffffff",
					"neutral": "#ffffff",
					"base-100": "#ffffff",
					"info": "#ffffff",
					"success": "#00ffff",
					"warning": "#ffffff",
					"error": "#ffffff",
				    "--rounded-box": "0",
				    "--rounded-btn": "0",
				    "--rounded-badge": "0",
				    "--tab-radius": "0",
				},
			},
		]
	},
	plugins: [require("@tailwindcss/typography"), require("daisyui")]
}