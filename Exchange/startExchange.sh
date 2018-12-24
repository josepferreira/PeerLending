protoc --java_out=. Ccs.proto
protoc --java_out=. Notificacao.proto
javac -cp protobuf-java-3.6.1.jar:. exchange/Ccs.java
javac -cp protobuf-java-3.6.1.jar:. exchange/NotificacaoOuterClass.java
javac -cp .:\* exchange/Proposta.java
javac -cp .:\* exchange/ExcecaoUltrapassado.java
javac -cp .:\* exchange/ExcecaoFinalizado.java
javac -cp .:\* exchange/Proposta.java
javac -cp .:\* exchange/Emprestimo.java
javac -cp .:\* exchange/Leilao.java
javac -cp .:\* exchange/Emissao.java
javac -cp .:\* exchange/Empresa.java
javac -cp .:\* exchange/EstruturaExchange.java
javac -cp .:\* exchange/Exchange.java 
./run exchange/Exchange