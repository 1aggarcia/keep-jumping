import "vitest";

interface CustomMatchers<R = unknown> {
  toHaveReceivedMessages: (messages: unknown[]) => Promise<unknown>;
  toReceiveMessage: (message: unknown) => Promise<unknown>;
}

declare module "vitest" {
  interface Matchers<T = any> extends CustomMatchers<T> {}
}
