/** Utility functions to help format strings and numbers */

const KB_SIZE = 1 << 10;
const MB_SIZE = 1 << 20;
const GB_SIZE = 1 << 30;
const MAX_SIG_FIGS = 4;

/**
 * Given a number of bytes, return a readable string such as
 * "25 B", "5 KB", "235 GB"
 * @param bytes integer
 */
export function formatBytesString(bytes: number) {
    bytes = Math.floor(bytes);

    // returns a number with an upper bound on the number of significant digits
    const truncateSigFigs = (n: number) => +n.toPrecision(MAX_SIG_FIGS);

    if (bytes > GB_SIZE) {
        return `${truncateSigFigs(bytes / GB_SIZE)} GB`;
    }
    if (bytes > MB_SIZE) {
        return `${truncateSigFigs(bytes / MB_SIZE)} MB`;
    }
    if (bytes > KB_SIZE) {
        return `${truncateSigFigs(bytes / KB_SIZE)} KB`;
    }
    return `${bytes} B`;
}

/**
 * Format a JSON string message with pretty indentation
 */
export function getPrettyMessage(message: unknown) {
    if (typeof message !== "string") {
        return `${message}`;
    }
    try {
        return JSON.stringify(JSON.parse(message), undefined, 2);
    } catch {
        return message;
    }
}
