const { VITE_SERVER_ENDPOINT } = import.meta.env;

export function getServerEndpoint() {
    if (VITE_SERVER_ENDPOINT === undefined) {
        console.warn("environment variable 'VITE_SERVER_ENDPOINT' is not set");
        return "ws://localhost:8081";
    }
    return VITE_SERVER_ENDPOINT;
}
