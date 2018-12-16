protoc --java_out=. Ccs.proto
javac -cp protobuf-java-3.6.1.jar:. cliente/Ccs.java
javac -cp protobuf-java-3.6.1.jar:. cliente/ClienteM.java
./run cliente/ClienteM