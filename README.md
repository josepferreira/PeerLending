# PeerLending

Distributed system for loan management, built using Erlang, Java and Dropwizard (REST).

How to get it running:

1. Run script to install required dependencies (Needs *sudo* permissions)

```console
sh downloadDependencies.sh
```
2. Start Diretorio.

```console
cd diretorio/Diretorio
java -jar target/Hello-1.0-SNAPSHOT.jar server hello.yml

```

3. Start all required Exchanges.

```console
cd Exchange
sh compilaProto.sh
sh compilaAndstart.sh exchange*.json
```
Different companies and ports need to detailed on JSON configuration files.

4. Start Frontend.

```console
cd FrontEnd
sh startFrontend.sh

```
Frontend also needs a configuration file. (Examples provided)

5. Start all the necessary clients.

```console
sh compilaProtoAndStart.sh
```
