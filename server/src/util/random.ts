export function randomHexColor() {
    return "#" + Math.floor(Math.random() * 16777215).toString(16);
}

export function randomInt(upperBound: number, step: number = 1) {
    return Math.floor(Math.random() * (upperBound / step)) * step;
}
