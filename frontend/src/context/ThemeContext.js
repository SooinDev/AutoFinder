import React, { createContext, useEffect } from "react";

// Site is permanently dark by design. Context kept for backwards
// compatibility with components that still consume it.
export const ThemeContext = createContext({
  darkMode: true,
  toggleDarkMode: () => {},
});

export const ThemeProvider = ({ children }) => {
  useEffect(() => {
    document.documentElement.classList.add("dark");
    document.documentElement.style.colorScheme = "dark";
  }, []);

  return (
    <ThemeContext.Provider value={{ darkMode: true, toggleDarkMode: () => {} }}>
      {children}
    </ThemeContext.Provider>
  );
};
