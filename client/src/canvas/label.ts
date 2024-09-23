import { Context2D } from "@lib/types";

const BLACK_HEX = "#000000";

export type LabelOptions = {
    text: string;
    x: number;
    y: number;
    textAlign?: CanvasTextAlign;
    font?: string;
}

export function renderLabel(context: Context2D, options: LabelOptions) {
    context.font = options.font ?? "15px Arial";
    context.textAlign = options.textAlign ?? "left";
    context.fillStyle = BLACK_HEX;
    context.fillText(options.text, options.x, options.y);
}
