# TODO

## Devops
- [ ] Type Check on build
- [x] CI linting
- [x] CI testing
- [x] Dev branch
- [x] CI auto deploy client
- [x] CI auto deploy server
- [x] CI test Java server
- [x] Java linter
- [ ] Reduce redundant checks on pull request
- [ ] Introduce unit tests for the frontend

## Server
- [x] Define data types for client controls
- [x] Set up game state
- [x] Build event loop for game
- [x] Handle client events in game state
- [x] Handle basic player movement
- [x] Optimize event loop by tracking moved players (Java server)
- [x] Rewrite server in Java
- [x] Fix concurrent printing issues
- [x] Fix idle thread bug: throws IllegalThreadStateException when loop is closed multiple times
- [x] Add support for moving platforms
- [x] Add gravity
- [ ] Add collisions for platforms
- [ ] Include more intelligent platform generation, based on location of nearby platforms
- [ ] Re-add support for game over

## Client
- [x] Create JS canvas for game
- [x] Build renderer for server messages
- [x] Build controller to send user input to server
- [ ] Add names to sprites
- [x] Render player color
- [x] Make rendered text bigger
- [x] Render message in/out states
- [ ] Add dark mode
- [x] Add rendering for platforms
- [*] Move all graphics inside the canvas
- [ ] Deprecate "GameJoinUpdate" message

## Other
- [x] Setup `lib` folder for shared models
- [ ] Find a way to share constants
- [ ] Reduce data sent over the network

# CLI
- Build docker image: `docker build -t websocket-game .`
- Test Maven app: `./mvnw test`
- Lint Maven app: `./mvnw checkstyle:check`
- Build Maven app: `./mvnw package` (output jar in `target/`)