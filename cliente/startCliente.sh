protoc --java_out=. Ccs.proto
protoc --java_out=. Notificacao.proto
javac -cp protobuf-java-3.6.1.jar:. cliente/Ccs.java
javac -cp protobuf-java-3.6.1.jar:. cliente/NotificacaoOuterClass.java
javac -cp .:\* cliente/GerirSubscricoes.java 
javac -cp .:\* cliente/Notificacoes.java
javac -cp .:\* cliente/ClienteM.java
./run cliente/ClienteM