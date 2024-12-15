## Server
The server receives various events from clients and updates Player state accordingly.
On a fixed interval, the server advances the game by one tick and broadcasts the game state to all clients.

### Events:
Events are JSON messages sent by clients. Clients also send events when they connect and disconnect. Events are handled using principles of functional programming. The primary abstraction for handling events is the following:
```
handle(event, gameState) -> { gameUpdate, reply }
```
Where `gameUpdate` is a data type describing how to modify the game state, and `reply` is an optional message to send back to the client. With this model, the bulk of event handling is performed in a purely functional way, with side effects limited to sending replies and applying `gameUpdate`s (simple puts, gets, deletes).

### Event Types:
The `handle` function has various implementations depending on the event. These are listed below:
- `handlePlayerJoin`: returns a new Player and client session to associate it to, or an error reply if the operation is not allowed
- `handlePlayerControl`: returns new player velocities, no reply
- `handlePlayerLeave`: returns a client session to remove, no reply
