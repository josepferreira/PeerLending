protoc --proto_path=../Proto --java_out=. CcsCliente.proto
protoc --proto_path=../Proto --java_out=. NotificacaoCliente.proto
javac -cp protobuf-java-3.6.1.jar:. cliente/Ccs.java
javac -cp protobuf-java-3.6.1.jar:. cliente/NotificacaoOuterClass.java
sh startCliente.sh