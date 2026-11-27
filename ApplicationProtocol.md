# Application Protocol

## Description
This application is a speed typing competition between multiple users. Each user, after having asked to join, gets placed in a game lobby automatically generated and managed. When the game starts, the users will be prompted with a text they will have to send back the server. After each game, every connection is broken and the clients will have to ask again to join a game again.

## Communications
### Transport protocol, port and encoding
This game uses TCP and the verification of the inputs are made client side as to not lose any messages and not having to send and reply every time the user types a letter. The server listens on port 1234.

Every message is encoded in UTF-8 and ends with a newline character (\n). A message is therefore one line of text, and cannot contain a newline. Parameters are separated by a single space. Keywords are written in capitals, and are case-sensitive.

The client opens the connection and speaks first, with a `JOIN` message. This will trigger the server to place him in a game lobby already made or make a new one and inform the client that he has been put in a lobby. In case the server cannot create a game lobby because every lobby slots are occupied, it will reply with an error.

The server will then send a `START` message with the text that the user will need to copy.

The client will then reply with a `COMMIT` message and the same text, in case the text is not the same, the server will send a `REDO` message until the client replies with a `COMMIT` with the right text.

After verifying the text, if it is right the server will send a `RESULT` message with the position of the client in the game.

At any time after the connection, the client can close the connection to leave.

Three standard errors are possible,`ERROR 1` for an unknown command, `ERROR 2`for a known command with a wrong format or arguments and finally `ERROR 3` for a known command input at the wrong moment.

A specific error is reserved for command specific errors, it is `ERROR 4`.


## Types, format and responses of messages

### Join a game
The client wants to join a game
#### Request
```
JOIN
```
#### Responses
- `ERROR 2`, known command, wrong format/arguments, arguments were added
- `ERROR 3`, known command, wrong moment, this client is already in a game lobby
- `ERROR 4`, the server could not connect the client to a game, all the lobbies are full
- `OK`, the client joined a game successfully


### Start
The server starts the game by giving a text and the start signal.
#### Request
```
START <text>
```
- `text`: text to be copied by the client
#### Responses
None

### Commit an answer
The client commits an answer by sending the copied text and want the server to validate the result.
#### Request
```
COMMIT <text>
```
- `text`: text copied by the client
#### Responses
- `ERROR 2`, known command, wrong format/arguments, no text given
- `ERROR 3`, known command, wrong moment, the game has not started yet or the client did not join any game
- `REDO`, The server expected another answer and the client needs to send another commit with the right text
- `RESULT <position>`, the text was the right one

### Give the result
The server gives the result of the game with the position of the player and then closes the communication
#### RESULT
```
RESULT <position>
```
- `position`: position that the player achieved
#### Responses
None


### Unknown or invalid message
The server answers any message it cannot serve.

#### Response
- `ERROR 1`: the message is unknown, or a parameter is missing or invalid.
- `ERROR 2`: known command, wrong format/arguments
- `ERROR 3`: known command, wrong moment
  The connection stays open in any cases.

## Global summary of the communication

|Who sends|Type of message|List of arguments|Description|
|:---:|:---:|:---:|:--|
|Client|JOIN|-|Asks to join a game|
|Server|START|text|`text`, text to copy|
|Client|COMMIT|text|`text`, text written|
|Server|REDO|-|Retry, wrong text given|
|Server|RESULT|position|`position`, position achieved in the game by the client|

## Examples
### Correctly made communications
```mermaid
sequenceDiagram
    Client1->>Server: JOIN
    Server-->>Client1:OK
    Client2->>Server: JOIN
    Server-->>Client2:OK
    Server->>Client1 :Start Hey jude, don't make it bad
    Server->>Client2 :Start Hey jude, don't make it bad
    Client2->>Server :COMMIT Hey jude
    Server-->>Client2:REDO
    Client2->>Server :COMMIT Hey jude, dont
    Server-->>Client2:REDO
    Client1->>Server :COMMIT Hey jude, don't make it wrongly
    Server-->>Client1:REDO
    Client1->>Server :COMMIT Hey jude, dont make it bad
    Server-->>Client1:REDO
    Client1->>Server :COMMIT Hey jude, don't make it bad any more
    Server-->>Client1:REDO
    Client2->>Server :COMMIT Hey jude, don't make it badly
    Server-->>Client2:REDO
    Client1->>Server :COMMIT Hey jude, don't make it bad
    Server->>Client1:RESULT 1
    Client2->>Server :COMMIT Hey jude, don't make it bad
    Server->>Client2:RESULT 2
```

### Wrongly made communications
#### wrong commands or format
```mermaid
sequenceDiagram
    Client1->>Server: COMMIT help me
    Server-->>Client1:ERROR 3
    Server-->Client1 : (connection cut)
    Client1->>Server: JOIN
    Server-->>Client1:OK
    Client2->>Server: JON
    Server-->>Client2:ERROR 1
    Server-->Client2 : (connection cut)
    Client2->>Server: JOIN 1
    Server-->>Client2:ERROR 2
    Server-->Client2 : (connection cut)
    Client2->>Server: JOIN
    Server-->>Client2:OK
    Server->>Client1 :Start Hey jude, don't make it bad
    Server->>Client2 :Start Hey jude, don't make it bad
    Client2->>Server :COMMIT Hey jude
    Server-->>Client2:REDO
    Client1->>Server: JOIN
    Server-->>Client1:ERROR 3
    Client2->>Server :COMMIT
    Server-->>Client2:ERROR 2
    Client1->>Server :COMMIT Hey jude, don't make it wrongly
    Server-->>Client1:REDO
    Client1->>Server :COMMIT Hey jude, dont make it bad
    Server-->>Client1:REDO
    Client1->>Server :COMMIT Hey jude, don't make it bad any more
    Server-->>Client1:REDO
    Client2->>Server :COMMIT Hey jude, don't make it badly
    Server-->>Client2:REDO
    Client1->>Server :COMMIT Hey jude, don't make it bad
    Server->>Client1:RESULT 1
    Client2->>Server :COMMIT Hey jude, don't make it bad
    Server->>Client2:RESULT 2
```
#### Servers are full
```mermaid
sequenceDiagram
    Client1->>Server: JOIN
    Server-->>Client1:ERROR 4
```

