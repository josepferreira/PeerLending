protoc --proto_path=../Proto --java_out=. CcsEx.proto
protoc --proto_path=../Proto --java_out=. NotificacaoEx.proto
javac -cp protobuf-java-3.6.1.jar:. exchange/CcsEx.java
javac -cp protobuf-java-3.6.1.jar:. exchange/NotificacaoEx.java