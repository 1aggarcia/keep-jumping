import globals from "globals";
import pluginJs from "@eslint/js";
import tseslint from "typescript-eslint";
import stylisticJs from "@stylistic/eslint-plugin-js";

export default [
    { files: ["**/*.{js,mjs,cjs,ts}"] },
    { languageOptions: { globals: globals.browser } },
    { ignores: ["node_modules", "dist", "src/generated"] },
    pluginJs.configs.recommended,
    ...tseslint.configs.recommended,
    { plugins: { "@stylistic/js": stylisticJs } },
    {
        rules: {
            semi: "error",
            "@stylistic/js/max-len": ["error", { "code": 80 }],
            "@stylistic/js/no-trailing-spaces": "error",
            "@stylistic/js/eol-last": ["error", "always"],
            "@stylistic/js/quotes": ["error", "double"],
        }
    }
];
