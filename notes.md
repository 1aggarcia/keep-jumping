# TODO

## Devops
- [ ] Type Check on build
- [x] CI linting
- [x] CI testing
- [ ] Dev branch

## Server
- [x] Define data types for client controls
- [x] Set up game state
- [x] Build event loop for game
- [x] Handle client events in game state
- [x] Handle basic player movement
- [ ] Fix build from producing `server` and `lib` folders
- [ ] Optimize event loop by tracking moved players

## Client
- [x] Create JS canvas for game
- [x] Build renderer for server messages
- [x] Build controller to send user input to server
- [ ] Create better-looking sprites
- [ ] Render player color
- [ ] Make rendered text bigger

## Other
- [x] Setup `lib` folder for shared models
- [ ] Find a way to share constants

# CLI
- Build docker image: `docker -t websocket-game .`