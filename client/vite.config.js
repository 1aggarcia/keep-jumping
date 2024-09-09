import { defineConfig } from "vite";

export default defineConfig({
    server: { host: "0.0.0.0" },
    base: "./",
    define: {
        LAST_UPDATED: new Date(),
    }
});
