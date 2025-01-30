import { gameElements } from "./dom";
import { AppState, Context2D } from "../types";
import { GAME_HEIGHT, GAME_WIDTH } from "./gameConstants";

const BUTTON_WIDTH = 170;
export const BUTTON_HEIGHT = 50;

const BORDER_RADIUS = 5;

const TEXT_COLOR = "white";
const FILL_COLOR = "blue";

export class Button {
    // properties of the button
    private text: string;
    private x = 0;
    private y = 0;
    private width = BUTTON_WIDTH;
    private height = BUTTON_HEIGHT;

    // button state
    private clickHandler = () => {};
    public isHovering = false;

    constructor(text: string) {
        this.text = text;
    }

    positionRight() {
        this.x = GAME_WIDTH - this.width;
        return this;
    }

    positionBottom() {
        this.y = GAME_HEIGHT - this.height;
        return this;
    }

    onClick(callback: () => void) {
        this.clickHandler = callback;
        return this;
    }

    click() {
        this.clickHandler();
    }

    render(context: Context2D) {
        context.roundRect(
            this.x, this.y, this.width, this.height, BORDER_RADIUS
        );
        if (this.isHovering) {
            context.fillStyle = TEXT_COLOR;
            context.strokeStyle = FILL_COLOR;
            context.stroke();
            context.fill();
        } else {
            context.fillStyle = FILL_COLOR;
            context.fill();
        }

        context.fillStyle = this.isHovering ? FILL_COLOR : TEXT_COLOR;
        context.textAlign = "center";
        context.textBaseline = "middle";
        context.font = "25px Arial";

        const textXPosition = this.x + (this.width / 2);
        const textYPosition = this.y + (this.height/ 2);
        context.fillText(this.text, textXPosition, textYPosition);
    }

    isPointTouchingButton(pointX: number, pointY: number) {
        return (
            this.x <= pointX
            && pointX <= this.x + this.width
            && this.y <= pointY
            && pointY <= this.y + this.height
        );
    }
}


/**
 * Subscribes the buttons passed in to cursor events for hovering and clicking
 * @param state - to set the buttons in the app state and find the context to
 *  draw to
 * @param buttons - list of buttons that should observe mouse events and
 *  be rendered
 */
export function subscribeButtonsToCursor(state: AppState, buttons: Button[]) {
    state.buttons = buttons;
    renderButtons(state.context, buttons);

    // the cursor might be hovering over a button and be a pointer
    gameElements.canvas[0].style.cursor = "default";

    // remove previous event handler, if there was any
    gameElements.canvas.off("mousemove");
    gameElements.canvas.on("mousemove", (event) => {
        const canvas = event.target;
        for (const button of buttons) {
            if (isCursorInsideButton(event, button) === button.isHovering) {
                continue;
            }
            if (button.isHovering) {
                button.isHovering = false;
                canvas.style.cursor = "default";
                button.render(state.context);
            } else {
                button.isHovering = true;
                canvas.style.cursor = "pointer";
                button.render(state.context);
            }
        }
    });

    gameElements.canvas.off("click");
    gameElements.canvas.on("click", (event) => {
        for (const button of buttons) {
            if (isCursorInsideButton(event, button)) {
                button.click();
            }
        }
    });
}

export function renderButtons(context: Context2D, buttons: Button[]) {
    for (const button of buttons) {
        button.render(context);
    }
}

/**
 * @param event JQuery mouse event
 * @param button
 * @returns `true` if the cursor position in the mouse event is inside the
 *  button based on the button properties, `false` otherwise
 */
function isCursorInsideButton(
    event: JQuery.MouseEventBase<HTMLCanvasElement>,
    button: Button
) {
    const canvas = event.target;
    const rect = canvas.getBoundingClientRect();

    const horizontalScale = canvas.width / rect.width;
    const verticalScale = canvas.width / rect.width;

    // needs to account for both offset based on the DOM element position,
    // and the scaling difference between the DOM and canvas
    const relativeCursorX = (event.clientX - rect.left) * horizontalScale;
    const relativeCursorY = (event.clientY - rect.top) * verticalScale;

    return button.isPointTouchingButton(relativeCursorX, relativeCursorY);
}
