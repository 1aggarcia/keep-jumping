import { ServerError } from "@lib/types";

/**
 * Syntactic sugar for a server error socket message object
 */
export function serverError(message: string): ServerError {
    return {
        type: "serverError",
        message: message,
    };
}
