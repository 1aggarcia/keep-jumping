# TODO

## Devops
- [ ] Type Check on build
- [x] CI linting
- [x] CI testing
- [x] Dev branch
- [x] CI auto deploy client
- [ ] CI auto deploy server
- [x] CI test Java server
- [x] Java linter

## Server
- [x] Define data types for client controls
- [x] Set up game state
- [x] Build event loop for game
- [x] Handle client events in game state
- [x] Handle basic player movement
- [ ] Fix build from producing `server` and `lib` folders
- [x] Optimize event loop by tracking moved players (Java server)
- [ ] Rewrite server in Java

## Client
- [x] Create JS canvas for game
- [x] Build renderer for server messages
- [x] Build controller to send user input to server
- [ ] Create better-looking sprites
- [x] Render player color
- [x] Make rendered text bigger
- [x] Render message in/out states

## Other
- [x] Setup `lib` folder for shared models
- [ ] Find a way to share constants

# CLI
- Build docker image: `docker -t websocket-game .`
- Test Maven app: `./mvnw test`
- Lint Maven app: `./mvnw checkstyle:check`