import { Context2D } from "./types";

const BLACK_HEX = "#000000";

export type LabelOptions = {
    text: string;
    x: number;
    y: number;
    textAlign?: CanvasTextAlign;
    textBaseline?: CanvasTextBaseline;
    font?: string;
    color?: string;
}

export function renderLabel(context: Context2D, options: LabelOptions) {
    context.font = options.font ?? "15px Arial";
    context.textAlign = options.textAlign ?? "left";
    context.textBaseline = options.textBaseline ?? "alphabetic";
    context.fillStyle = options.color ?? BLACK_HEX;
    context.fillText(options.text, options.x, options.y);
}
