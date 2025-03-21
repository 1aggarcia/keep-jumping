/// <reference types="vite/client" />

declare const VERSION: string;

interface ImportMetaEnv {
    readonly VITE_WEBSOCKET_ENDPOINT?: string;
    readonly VITE_HTTP_ENDPOINT?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
