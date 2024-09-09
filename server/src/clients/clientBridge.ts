import { WebSocket } from "ws";
import { randomInt } from "../util/random";

// helps identify specific instances of the server in the cloud
const instanceId = randomInt(999).toString().padStart(3, "0");

const localClientStore = new Map<string, WebSocket>();

let nextId = 0;

export interface ClientBridge {
    /**
     * @returns the number of clients in the communication bridge
     */
    readonly size: () => number;

    /**
     * Adds a new client to the communication bridge
     * @param socket the client
     * @returns the ID of the client
     */
    readonly addClient: (socket: WebSocket) => string;

    /**
     * Removes the client from the communication bridge
     * @param id the ID of the client
     * @returns success
     */
    readonly removeClient: (id: string) => boolean;

    /**
     * Sends a message to all clients in the communication bridge
     * @param message should be JSON serializable (can be plain string)
     */
    readonly broadcast: <T>(jsonMessage: T) => void;

    /**
     * Sends a message to a client of the given id
     * @param message should be JSON serializable (can be plain string)
     * @param clientId the client to send it to
     */
    readonly send: <T>(message: T, clientId: string) => void;
}

export const clientBridge: ClientBridge = {
    size: () => localClientStore.size,

    addClient: (socket) => {
        const id = getNextId();
        localClientStore.set(id, socket);
        return id;
    },

    removeClient: (id) => localClientStore.delete(id),

    broadcast: (message) => {
        for (const client of localClientStore.values()) {
            client.send(JSON.stringify(message));
        }
    },

    send: (message, clientId) => {
        const client = localClientStore.get(clientId);
        if (client === undefined) {
            throw new ReferenceError(`Client ${clientId} doesn't exist`);
        }
        client.send(JSON.stringify(message));
    },
};

/**
 * @returns The next unused id
 */
function getNextId() {
    // TODO: make id generation random for security
    const id = nextId.toString();
    nextId++;
    return instanceId + ":" + id;
}
