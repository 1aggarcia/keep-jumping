import { defineConfig } from "vite";

export default defineConfig({
    server: { host: "0.0.0.0" },
    base: "./",
    define: {
        VERSION: (() => {
            const now = new Date();
            const paddedMonth = `${now.getUTCMonth() + 1}`.padStart(2, "0");
            const paddedDay = `${now.getUTCDate()}`.padStart(2, "0");

            // without the quotes, vite treats this as a number
            return `"${now.getUTCFullYear()}.${paddedMonth}${paddedDay}"`;
        })(),
    }
});
