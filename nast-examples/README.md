# Examples

## Echo

### Server

run ``com.fnklabs.nast.examples.echo.EchoServer``

Server will bound on 127.0.0.1:10000 by default

### Client

run ```com.fnklabs.nast.examples.echo.EchoClient```

Client try to connect to 127.0.0.1:10000

So you could type any text then press "Enter" and see reply message

````
Hello world
retrieve -1 > Hello world
```` 

## Chat

run ``com.fnklabs.nast.examples.chat.ChatServer``

Server will bound on 127.0.0.1:10000 by default

### Client

run twice or more ```com.fnklabs.nast.examples.chat.ChatClient```

Client try to connect to 127.0.0.1:10000

So you could type any text then press "Enter" and see  messages on on clients

Client 1
````
Hello world \n
retrieve -1 > Hello world
````

Client 2
````
retrieve -1 > Hello world
````  