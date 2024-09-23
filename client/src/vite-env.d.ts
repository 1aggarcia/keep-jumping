/// <reference types="vite/client" />

declare const VERSION: string;

interface ImportMetaEnv {
    readonly VITE_SERVER_ENDPOINT?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
