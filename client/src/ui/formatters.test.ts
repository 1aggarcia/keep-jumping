import { describe, expect, it } from "vitest";
import { getMessageStatsText } from "./formatters";
import { mockState } from "../testUtils";

describe(getMessageStatsText, () => {
    it("shows all stats from app state", () => {
        const state = mockState({
            messagesOut: 1,
            messagesIn: 2,
            bytesIn: 3,
        });
        const text = getMessageStatsText(state);
        expect(text).toEqual(
            "Sent: 1 | Received: 2 | Data in: 3 B | Mean ping size: 2 B");
    });

    it("shows bytes at correct cutoff", () => {
        const state = mockState({ bytesIn: 1024 });
        expect(getMessageStatsText(state)).toContain("1024 B");
    });

    it("shows kilobytes at correct cutoff", () => {
        const state = mockState({ bytesIn: 1_048_576 });
        expect(getMessageStatsText(state)).toContain("1024 KB");
    });

    it("shows megabytes at correct cutoff", () => {
        const state = mockState({ bytesIn: 1_073_741_824 });
        expect(getMessageStatsText(state)).toContain("1024 MB");
    });

    it("shows gigabytes at correct cutoff", () => {
        const state = mockState({ bytesIn: 1_073_741_825 });
        expect(getMessageStatsText(state)).toContain("1 GB");
    });
});
