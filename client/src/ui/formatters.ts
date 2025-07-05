/** Utility functions to help format strings and numbers */

// TODO write tests 

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

const MS_PER_SECOND = 1000;
const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
const SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;
const SECONDS_PER_WEEK = SECONDS_PER_DAY * 7;
const SECONDS_PER_YEAR = SECONDS_PER_WEEK * 52;

/**
 * Returns a string such as "5 months ago" or "3 minutes ago" depending on
 * how much time has passed since the passed in date
 * @param timestamp
 */
export function getRelativeAgeString(timestamp: Date) {
    const timestampSeconds = timestamp.getTime() / MS_PER_SECOND;
    const nowSeconds = Date.now() / MS_PER_SECOND;
    if (timestampSeconds > nowSeconds) {
        console.error(`Tried to get age of future timestamp: ${timestamp}`);
        return timestamp.toLocaleDateString();
    }
    const secondsPassed = nowSeconds - timestampSeconds;

    let divisor: number;
    let unit: string;
    if (secondsPassed < SECONDS_PER_MINUTE) {
        divisor = 1;
        unit = "second";
    } else if (secondsPassed < SECONDS_PER_HOUR) {
        divisor = SECONDS_PER_MINUTE;
        unit = "minute";
    } else if (secondsPassed < SECONDS_PER_DAY) {
        divisor = SECONDS_PER_HOUR;
        unit = "hour";
    } else if (secondsPassed < SECONDS_PER_WEEK) {
        divisor = SECONDS_PER_DAY;
        unit = "day";
    } else if (secondsPassed < SECONDS_PER_YEAR) {
        divisor = SECONDS_PER_WEEK;
        unit = "week";
    } else {
        divisor = SECONDS_PER_YEAR;
        unit = "year";
    }
    const timeInterval = Math.floor(secondsPassed / divisor);
    const pluralIndicator = timeInterval === 1 ? "" : "s";

    return `${timeInterval} ${unit}${pluralIndicator} ago`;
}
