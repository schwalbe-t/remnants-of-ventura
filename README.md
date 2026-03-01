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
- `VENTURA_BIGTON_LIB_DIR` - The relative or absolute directory containing the compiled dynamic library
for the BIGTON runtime (defaults to `bigtonruntime/build/libs/main`). The server
will look for a different file depending on the host operating system:
  - Linux - `libbigtonruntime.so`
  - MacOS - `libbigtonruntime.dylib`
  - Windows - `bigtonruntime.dll`

### Running the Client
The client can be built and run using Gradle:
```
./gradlew :client:run
```

The client can be configured using the following environmental variables:
- `VENTURA_TRUST_ALL_CERTS` - (if set to `true`) Makes the client trusts ALL certificates. ONLY USE FOR TESTING!

### Environment Variables

Both the server and client can be configurd using a traditional `.env` file
at the repository root, containing `<VAR>=<VALUE>` on each line of the file.
For example, to have the server run on port 844 and the client trust all
certificates, `.env` could be defined as such:
```
VENTURA_DB_PASS=<PASSWORD>
VENTURA_PORT=844
VENTURA_TRUST_ALL_CERTS=true
```

### Creating Client Release Builds

Note: The following instructions assume a POSIX shell environment.

To create a client release build, change the current directory to `client`
and run the `build.sh` shell script:
```sh
cd client
./build.sh windows  # create standalone windows x86 build
./build.sh linux    # create standalone linux x86 build
```

Executing these commands will download the JDK for the chosen platform once and
keep them in `client/builds/jdk/<OS>/`. Unless deleted, the script won't
download them again.

The actual built versions of the application are placed into
`client/builds/<OS>/`.

### Creating Windows Installer

Note: The following instructiosn assume a Windows environment with
[Inno Setup](https://jrsoftware.org/isinfo.php) installed at
`C:\Program Files (x86)\Inno Setup 6\`.

To create a windows installer script, run the following command:
```
"C:\Program Files (x86)\Inno Setup 6\ISCC.exe" /DVersion=<VERSION> /DSrcDir=<BUILD-DIR> remnants-of-ventura.iss
```
- Replace `<VERSION>` with the build version, e.g. `0.1.0`.
- Replace `<BUILD-DIR>` with the absolute path of the directory containing a Windows build produced by `client/build.sh`.
- If not present in the current directory, replace `remnants-of-ventura.iss` with the path to the Inno Setup Script (located at `client/remnants-of-ventura.iss` in this repository)