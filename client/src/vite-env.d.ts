/// <reference types="vite/client" />

declare const LAST_UPDATED: string;

interface ImportMetaEnv {
    readonly VITE_SERVER_ENDPOINT?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
