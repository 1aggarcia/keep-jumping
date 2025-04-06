# TODO

## Devops
- [ ] Reduce redundant checks on pull request
- [ ] Introduce unit tests for the frontend
- [ ] Add database credentials to CI/CD for automated deployment
- [*] Switch from fly.io to Google Cloud Run to host server
- [x] Type Check on build
- [x] CI linting
- [x] CI testing
- [x] Dev branch
- [x] CI auto deploy client
- [x] CI auto deploy server
- [x] CI test Java server
- [x] Java linter

## Server
- [ ] Don't send redundant information (old platforms) to reduce ping size (current avg 90 B)
- [ ] Re-add support for game over
- [x] Include more intelligent platform generation, based on location of nearby platforms
- [x] Add seperate dev and prod leaderboards
- [x] Detect player passing through platforms between two frames
- [x] Add persistent storage for user scores
- [x] Create leaderboard ~~Data Access Object~~ service
- [x] Create SQL database and connect to service
- [x] Add REST endpoints for leaderboard
- [x] Send server ID to client on join
- [x] Increase speed with time
- [x] Always spawn players on or above a platform
- [x] Migrate string JSON messages to binary Protobuf messages
- [x] Allow players to jump off screen
- [x] Use `JsonTypeInfo` and `JsonSubTypes` from Jackson for deserializing JSON union types (maybe use Protobuf instead?)
- [x] Refactor dynamic message handler to dispatch to correct handler
- [x] Make player names read only
- [x] Add collisions for platforms
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
- [ ] Keep websocket connection open across games, create "LeaveEvent" to leave a game
- [ ] Add dark mode
- [*] Process disconnect requests / other errors
- [x] Use HTTP for health check
- [x] Fetch and show leaderboard when not connected
- [*] Move all graphics inside the canvas
- [x] Design leaderboard UI
- [x] Reduce errors for closed servers
- [x] Show errors to user
- [x] Add leaderboard rendering
- [x] Deprecate "GameJoinUpdate" message
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
- [*] Reduce data sent over the network
- [x] Setup `lib` folder for shared models

# CLI
- Build docker image: `docker build -t keep-jumping .`
- Test Maven app: `./mvnw test`
- Lint Maven app: `./mvnw checkstyle:check`
- Build Maven app: `./mvnw package` (output jar in `target/`)
- Deploy to GCP: `gcloud run deploy ${SERVICE} --source .`

