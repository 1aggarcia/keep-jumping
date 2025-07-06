import { describe, expect, it } from "vitest";
import { buildLeaderboardRows } from "./dom";

describe(buildLeaderboardRows, () => {
    it("should contain 10 rows", () => {
        expect(buildLeaderboardRows()).toHaveLength(10);
    });
});
