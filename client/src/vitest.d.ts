import "vitest";

interface CustomMatchers<R = unknown> {
  toHaveReceivedMessages: (messages: unknown[]) => unknown;
  toReceiveMessage: (message: unknown) => unknown;
}

declare module "vitest" {
  interface Matchers<T = any> extends CustomMatchers<T> {}
}
