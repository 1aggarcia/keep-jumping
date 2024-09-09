# Websocket Game 

A simple, TypeScript based Websocket game for learning purposes
Server runs on [ws](https://github.com/websockets/ws) for Node.js, frontend on jQuery (just for the memes).

## Setup & Build

Requires [Node v20](https://nodejs.org/en) (verify installation with `node -v`)

Developed with [pnpm](https://pnpm.io/) but should work fine with npm, which comes with Node.

### Server:
`cd server`

- Run: `npm run dev`
- Build: `npm run build`
- Test: `npx tap run`

### Frontend
`cd client`

- Run: `npm run dev`
- Build: `npm run build`

The frontend looks for a server running on `localhost` by default. To specify a different server, create a `.env` file and set the `VITE_SERVER_ENDPOINT` enviornment variable to the desired server host.
