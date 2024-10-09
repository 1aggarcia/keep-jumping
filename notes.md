# TODO

## Devops
- [ ] Reduce redundant checks on pull request
- [ ] Introduce unit tests for the frontend
- [x] Type Check on build
- [x] CI linting
- [x] CI testing
- [x] Dev branch
- [x] CI auto deploy client
- [x] CI auto deploy server
- [x] CI test Java server
- [x] Java linter

## Server
- [ ] Refactor dynamic message handler to dispatch to correct handler
- [ ] Add persistent storage for user scores
- [ ] Add collisions for platforms
- [ ] Include more intelligent platform generation, based on location of nearby platforms
- [ ] Re-add support for game over
- [x] Add processing for player scores
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

## Client
- [ ] Add dark mode
- [ ] Deprecate "GameJoinUpdate" message
- [*] Move all graphics inside the canvas
- [x] Add scores to sprites
- [x] Add names to sprites
- [x] Create JS canvas for game
- [x] Build renderer for server messages
- [x] Build controller to send user input to server
- [x] Render player color
- [x] Make rendered text bigger
- [x] Render message in/out states
- [x] Add rendering for platforms

## Other
- [ ] Find a way to share constants
- [ ] Reduce data sent over the network
- [x] Setup `lib` folder for shared models

# CLI
- Build docker image: `docker build -t websocket-game .`
- Test Maven app: `./mvnw test`
- Lint Maven app: `./mvnw checkstyle:check`
- Build Maven app: `./mvnw package` (output jar in `target/`)