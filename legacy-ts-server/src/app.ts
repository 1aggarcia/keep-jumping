import WebSocket from "ws";
import {
    onClientDisconnect,
    onClientMessage,
    setUpClient
} from "./clients/clientHandler";
import { clientBridge } from "./clients/clientBridge";
import { LocalGameRepository } from "./game/localGameRepository";

const PORT = 8081;
const IDLE_TIME_LIMIT = 15 * 60 * 1000;  // 15 minutes

const server = new WebSocket.Server({ host: "0.0.0.0", port: PORT });
const game = new LocalGameRepository();

let idleTimeout: NodeJS.Timeout;

server.on("listening", () => {
    // so that the server doesn't run forever
    idleTimeout = setTimeout(closeServer, IDLE_TIME_LIMIT);
    console.log(`Listening on port ${PORT}`);
});

server.on("connection", (socket) => {
    const clientId = setUpClient(socket, clientBridge, game);
    socket.on("message", (message) => {
        // server should stay alive as long as there are active connections
        clearTimeout(idleTimeout);
        onClientMessage(clientId, message, clientBridge, game);
    });
    socket.on("close", () => {
        onClientDisconnect(clientId, clientBridge, game);
        if (clientBridge.size() === 0) {
            idleTimeout = setTimeout(closeServer, IDLE_TIME_LIMIT);
        }
    });
});

function closeServer() {
    console.log(
        `Idle time limit reached (${IDLE_TIME_LIMIT} ms).`
        + " Shutting down server..."
    );
    server.close();
}
