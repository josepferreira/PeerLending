# PeerLending
### Repositório do Trabalho Prático de PSD

Para colocar o sistema a funcionar :


1. Correr a script para obter as dependências. Vai ser necessário acesso privilegiado ( *sudo* )

```console
sh downloadDependencies.sh
```
2. Executar o Diretorio.

3. Executar as Exchanges.

```console
cd Exchange
sh compilaProto.sh
sh compilaAndstart.sh exchange*.json
```
Substituir * pelo numero da exchange.

4. Executar o FrontEnd.

```console
cd FrontEnd
sh startFrontend.sh
```

5. Executar os clientes necessários.

```console
sh compilaProtoAndStart.sh
```
