# server must be built first with JS output in "dist" folder

FROM node:current-alpine

# based on the OS
WORKDIR /app

COPY node_modules node_modules
COPY dist .

ENV PORT=8081

CMD [ "node", "server/src/app.js" ]