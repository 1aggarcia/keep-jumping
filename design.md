## Server
The server receives various events from clients and updates Player state accordingly.
On a fixed interval, the server advances the game by one tick and broadcasts the game state to all clients.

### Events:
Events are JSON messages sent by clients. Clients also send events when they connect and disconnect. Events are handled using principles of functional programming. The primary abstraction for handling events is the following:
```
process(event, gameState) -> { gameUpdate, reply }
```
Where `gameUpdate` is a data type describing how to modify the game state, and `reply` is an optional message to send back to the client. With this model, the bulk of event processing is performed in a purely functional way, with the result determined exclusively by the inputs. Side effects are performed outside the `process` function and are limited to sending replies and applying `gameUpdate`s (simple puts, gets, deletes).

### Event Types:
The `process` function has various implementations depending on the event. These are listed below:
- `processPlayerJoin`: returns a new Player and client session to associate it to, or an error reply if the operation is not allowed
- `processPlayerControl`: returns new player velocities, no reply
- `processPlayerLeave`: returns a client session to remove, no reply
