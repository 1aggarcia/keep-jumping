type TestConfig = {
    name: string;
    strategy: (arr: { num?: number }[]) => number[];
};

function runPerformanceTest(config: TestConfig, arrayLength: number) {
    const input = Array.from({ length: arrayLength })
        .map((_, idx) => ({
            // about 50% of the elements should be undefined
            num: Math.random() > 0.5 ? idx : undefined
        })
    ); 
    const label = `${arrayLength.toLocaleString()} elements - ${config.name}`;
    const start = performance.now();
    config.strategy(input);
    const end = performance.now();

    return { label, time: end - start };
}

const LENGTHS = [100, 10_000, 10_000_000];
const TESTS: TestConfig[] = [
    {
        name: "map + filter",
        strategy: arr => arr
            .map(obj => obj.num)
            .filter(obj => obj !== undefined),
    },
    {
        name: "flatMap - always return array",
        strategy: arr => arr.flatMap(obj => {
            const num = obj.num;
            return num ? [num] : []
        }),
    },
    {
        name: "flatMap - only return array in empty case",
        strategy: arr => arr.flatMap(obj => obj.num ?? []),
    },
    {
        name: "reduce",
        strategy: arr => arr.reduce<number[]>((res, obj) => {
            const num = obj.num;
            if (num) {
                res.push(num);
            }
            return res;
        }, [])
    },
    {
        name: "forEach method",
        strategy: arr => {
            const res: number[] = [];
            arr.forEach(obj => {
                const num = obj.num;
                if (num) {
                    res.push(num);
                }
            });
            return res;
        }
    },
    {
        name: "traditional for loop",
        strategy: arr => {
            const res: number[] = [];
            for (const obj of arr) {
                const num = obj.num;
                if (num) {
                    res.push(num);
                }
            }
            return res;
        }
    }
];

console.log("running tests...");
console.time("total test time");
const results = LENGTHS.flatMap(length =>
    TESTS.map(test => runPerformanceTest(test, length))
);
console.timeEnd("total test time");
results.sort((a, b) => a.time - b.time);

console.log("");
for (const res of results) {
    console.log(`${res.time.toFixed(4)} ms`);
    console.log(`- ${res.label}`);
}
