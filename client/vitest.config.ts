import { configDefaults, defineConfig } from 'vitest/config'

export default defineConfig({
    test: {
        environment: "jsdom",
        coverage: {
            enabled: true,
            provider: "v8",
            reporter: "text",
            exclude: [
                ...configDefaults.exclude,
                "**/src/generated/**"
            ]
        },
    }
});
