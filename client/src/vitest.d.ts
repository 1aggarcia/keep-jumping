import "vitest";

interface CustomMatchers<R = unknown> {
    toHaveReceivedMessages: (messages: unknown[]) => Promise<unknown>;
    toReceiveMessage: (message: unknown) => Promise<unknown>;
}

declare module "vitest" {
    interface Matchers<T = any> extends CustomMatchers<T> {}
}

// from jest-canvas-mock, types not supported by vitest-canvas-mock
// https://github.com/hustcc/jest-canvas-mock/blob/master/types/index.d.ts
interface CanvasRenderingContext2DEvent {
    /**
     * This is the type of canvas event that occurred.
     */
    type: string;
    /**
     * This is a six element array that contains the current state of the canvas `currentTransform`
     * value.
     */
    transform: [number, number, number, number, number, number];
    /**
     * These are the relevant properties related to this canvas event.
     */
    props: {
        [key: string]: any;
    };
}

declare global {
    interface CanvasRenderingContext2D {
        /**
         * Get all the events associated with this CanvasRenderingContext2D object.
         *
         * This method cannot be used in a production environment, only with `jest` using
         * `jest-canvas-mock` and should only be used for testing.
         *
         * @example
         * expect(ctx.__getEvents()).toMatchSnapshot();
         */
        __getEvents(): CanvasRenderingContext2DEvent[];
    }
}
