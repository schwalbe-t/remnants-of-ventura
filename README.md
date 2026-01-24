# remnants-of-ventura
A game about exploring ruins and building and programming robots.

### Database Setup
You can use Docker to create a local PostgreSQL instance:
```
mkdir .postgresql
docker run -d \
    --name ventura-postgres \
    -e POSTGRES_USER=ventura \
    -e POSTGRES_PASSWORD=<PASSWORD> \
    -e POSTGRES_DB=ventura \
    -p 5432:5432 \
    -v $PWD/.postgresql:/var/lib/postgresql/data \
    postgres:16
```
Note: on SELinux-based distros (Fedora, Red Hat, CentOS, ...) the line needs to be
```
    -v $PWD/.postgresql:/var/lib/postgresql/data:z \
```
(note the `:z` at the end)

Docker can then be used to start, stop or restart the database:
```
docker start ventura-postgres
docker restart ventura-postgres
docker stop ventura-postgres
```

### Running the Server
The server can be built and run using Gradle:
```
./gradlew :server:run
```

Note that by default the server will use the self-signed certificate in `server/dev-keystore.p12`. The client can be made to trust it using an envionmental variable.

The server can be configured using the following environmental variables:
- `VENTURA_KEYSTORE_PATH` - Path of the `.p12` SSL certificate (defaults to `dev-keystore.p12`)
- `VENTURA_KEYSTORE_ALIAS` - Alias of the certificate in the keystore (defaults to `ventura`)
- `VENTURA_KEYSTORE_PASS` - Password for the keystore (defaults to `labubu`)
- `VENTURA_PORT` - Port to listen for clients on (defaults to `8443`)
- `VENTURA_DB_URL` - Url of the database (defaults to `jdbc:postgresql://localhost:5432/ventura`)
- `VENTURA_DB_USER` - Username in the database (defaults to `ventura`)
- `VENTURA_DB_PASS` - Password for the database **(has no default)**

### Running the Client
The client can be built and run using Gradle:
```
./gradlew :client:run
```

The client can be configured using the following environmental variables:
- `VENTURA_TRUST_ALL_CERTS` - (if set to `true`) Makes the client trusts ALL certificates. ONLY USE FOR TESTING!