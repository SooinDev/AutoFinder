/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // surface (dark neutral)
        bg: {
          DEFAULT: "#0E0E10",
          raised: "#16161A",
          inset: "#1C1C21",
          hover: "#22222A",
        },
        // border
        line: {
          DEFAULT: "#26262E",
          strong: "#33333D",
        },
        // text
        fg: {
          DEFAULT: "#EDEDF0",
          muted: "#A1A1AA",
          subtle: "#71717A",
          faint: "#52525B",
        },
        // accent
        brand: {
          DEFAULT: "#7C9CFF",
          hover: "#A4BBFF",
          subtle: "rgba(124, 156, 255, 0.12)",
        },
        // semantic
        success: "#34D399",
        warning: "#FBBF24",
        danger: "#F87171",
      },
      fontFamily: {
        sans: [
          "Inter",
          "Pretendard",
          "-apple-system",
          "BlinkMacSystemFont",
          "Apple SD Gothic Neo",
          "Segoe UI",
          "Roboto",
          "system-ui",
          "sans-serif",
        ],
        mono: [
          "ui-monospace",
          "SFMono-Regular",
          "Menlo",
          "Monaco",
          "Consolas",
          "monospace",
        ],
      },
      fontSize: {
        "display-2xl": ["3.75rem", { lineHeight: "1.05", letterSpacing: "-0.025em" }],
        "display-xl": ["3rem", { lineHeight: "1.1", letterSpacing: "-0.02em" }],
        "display-lg": ["2.25rem", { lineHeight: "1.15", letterSpacing: "-0.015em" }],
      },
      borderRadius: {
        DEFAULT: "8px",
        md: "10px",
        lg: "14px",
        xl: "18px",
      },
      boxShadow: {
        card: "0 1px 0 rgba(255,255,255,0.04) inset, 0 1px 2px rgba(0,0,0,0.5)",
        lift: "0 8px 24px rgba(0,0,0,0.4), 0 1px 0 rgba(255,255,255,0.05) inset",
      },
      maxWidth: {
        container: "1240px",
      },
      transitionTimingFunction: {
        smooth: "cubic-bezier(0.4, 0, 0.2, 1)",
      },
    },
  },
  plugins: [require("@tailwindcss/forms")],
};
