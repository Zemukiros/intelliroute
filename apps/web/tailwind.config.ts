import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: "#0b1220",
        panel: "#111a2e",
        accent: "#38bdf8",
        "accent-strong": "#0ea5e9",
      },
    },
  },
  plugins: [],
};

export default config;
