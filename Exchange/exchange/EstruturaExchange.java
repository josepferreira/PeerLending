package exchange;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.zeromq.ZMQ;

import exchange.Ccs.*;
import exchange.NotificacaoOuterClass.*;

class EmprestimoProntoTerminar implements Comparable{
    public Emprestimo emprestimo;

    public EmprestimoProntoTerminar(Emprestimo e){
        emprestimo = e;
    }

    public int compareTo(Object o){
        EmprestimoProntoTerminar ept = (EmprestimoProntoTerminar)o;
        return this.emprestimo.fim.compareTo(ept.emprestimo.fim);
    }

    public boolean equals(Object o){
        if(o==null){
            return false;
        }

        if(!(o instanceof EmprestimoProntoTerminar)){
            return false;
        }

        EmprestimoProntoTerminar ept = (EmprestimoProntoTerminar)o;

        return ept.emprestimo.equals(this.emprestimo);
    }
}


class EstruturaExchange{
    public TreeSet<EmprestimoProntoTerminar> paraTerminar = new TreeSet<>();
    public HashMap<String,Empresa> empresas = new HashMap<>();
    public Thread acaba;
    public ZMQ.Context context;
    public ZMQ.Socket socketExchangePush;
    public ZMQ.Socket socketNotificacoes;
    //so falta o diretorio

    public EstruturaExchange(ZMQ.Context c, String myPush, String myPub){
        context = c;
        socketNotificacoes = context.socket(ZMQ.PUB);
        socketExchangePush = context.socket(ZMQ.PUSH);
        socketExchangePush.bind("tcp://*:" + myPush);
        socketNotificacoes.bind("tcp://*:" + myPub);
        
        empresas.put("emp1", new Empresa("emp1"));
        empresas.put("emp2", new Empresa("emp2"));

        System.out.println("Estrutura configurada");

    }
    private void possoInterromper(){
        acaba.interrupt();
    }
    
    //tempo em dias para minutos (1 dia corresponde a 1 minuto), devolve em nanos
    private long converteTempo(long tempo){
        
        return (long)(((tempo*Math.pow(10, 9)*12)));///(24*60));
    }

    public synchronized boolean adicionaEmissao(String empresa, long montante, long tempo){
        long tempoAux = converteTempo(tempo);
        System.out.println("TEMPO: " + tempoAux);
        LocalDateTime fim = LocalDateTime.now().plusNanos(tempoAux);
        Emprestimo aux = empresas.get(empresa).criarEmissao(montante,fim);
        if(aux != null){
            EmprestimoProntoTerminar ept = new EmprestimoProntoTerminar(aux);
            if(paraTerminar.isEmpty() || ept.equals(paraTerminar.first())){
                possoInterromper();
            }
            paraTerminar.add(ept);
            System.out.println("Adicionei: " + aux.empresa);
            System.out.println("Adicionei: " + ept.emprestimo.empresa);
            //manda para o diretorio
            Resposta respostaEmissao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.EMISSAO)
                                            .setUtilizador(empresa)
                                            .setSucesso(true)
                                            .setMensagem("Emissao criada com sucesso!")
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOSTA)
                                            .setResposta(respostaEmissao)
                                            .build();

            socketExchangePush.send(respostaFinal.toByteArray());

            Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipo(TipoNotificacao.CRIACAO)
                                            .setTipoMensagem(TipoAcao.EMISSAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(aux.taxa)
                                            .setTempo(tempo)
                                            .build();

            String topic = "emissao::" + empresa + "::";
            socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
            socketNotificacoes.send(notificacao.toByteArray());

            return true;
        }
        Resposta respostaEmissao = Resposta.newBuilder()
                            .setTipo(TipoMensagem.EMISSAO)
                            .setUtilizador(empresa)
                            .setSucesso(false)
                            .setMensagem("Emissao não foi criada com sucesso!")
                            .build();
        RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                            .setTipo(TipoResposta.RESPOSTA)
                            .setResposta(respostaEmissao)
                            .build();
        socketExchangePush.send(respostaFinal.toByteArray());

        return false;
    }

    public synchronized boolean licitaEmissao(String empresa, long montante, String investidor){
        try{
            Empresa aux = empresas.get(empresa);
            if(aux == null){
                return false;
            }
            Emissao em = aux.licitaEmissao(investidor, montante);

            if(em != null){
                //indica que emissao terminou
                Resultado resultado = Resultado.newBuilder()
                                                .setTipo(TipoMensagem.EMISSAO)
                                                .setEmpresa(em.empresa)
                                                .setTexto(em.propostas.toString())
                                                .build();
                RespostaExchange resultadoFinal = RespostaExchange.newBuilder()
                                                .setTipo(TipoResposta.RESULTADO)
                                                .setResultado(resultado)
                                                .build();
                socketExchangePush.send(resultadoFinal.toByteArray());
                //tem de enviar notificacao
                ResultadoAcao resultadoA = ResultadoAcao.newBuilder()
                                                .setTipo(TipoAcao.EMISSAO)
                                                .setTexto(em.toString())
                                                .build();

                Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipo(TipoNotificacao.FIM)
                                            .setTipoMensagem(TipoAcao.EMISSAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setResultado(resultadoA)
                                            .build();

                String topic = "emissao::" + empresa + "::";
                socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
                socketNotificacoes.send(notificacao.toByteArray());
                    
                //tem de enviar para o diretorio

            }
            
            else{
                //responde apenas ao cliente
                Resposta respostaEmissao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.EMISSAO)
                                            .setUtilizador(investidor)
                                            .setSucesso(true)
                                            .setMensagem("A sua licitação para uma emissão, de " + montante + ", para a empresa " + empresa + ", foi adicionada com sucesso")
                                            .build();
                RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                                .setTipo(TipoResposta.RESPOSTA)
                                                .setResposta(respostaEmissao)
                                                .build();
                socketExchangePush.send(respostaFinal.toByteArray());
                
                //tem de enviar notificacao
                Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipo(TipoNotificacao.LICITACAO)
                                            .setTipoMensagem(TipoAcao.EMISSAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .build();
                String topic = "emissao::" + empresa + "::";
                socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
                socketNotificacoes.send(notificacao.toByteArray());
                                                
            }
        }
        catch(ExcecaoFinalizado ef){
            //indica que já se encontra terminado
            Resposta respostaEmissao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.EMISSAO)
                                            .setUtilizador(investidor)
                                            .setSucesso(true)
                                            .setMensagem("Para a empresa: " + ef.empresa + ": " + ef.mensagem)
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOSTA)
                                            .setResposta(respostaEmissao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
        }
        return true;
    }

    public synchronized boolean adicionaLeilao(String empresa, long montante, float taxa, long tempo){
        LocalDateTime fim = LocalDateTime.now().plusNanos(converteTempo(tempo));
        Emprestimo aux = empresas.get(empresa).criarLeilao(montante, taxa, fim);
        if(aux != null){
            EmprestimoProntoTerminar ept = new EmprestimoProntoTerminar(aux);
            if(paraTerminar.isEmpty() || ept.equals(paraTerminar.first())){
                possoInterromper();
            }
            paraTerminar.add(ept);
            //manda para o diretorio
            Resposta respostaLeilao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.LEILAO)
                                            .setUtilizador(empresa)
                                            .setSucesso(true)
                                            .setMensagem("Leilão criado com sucesso!")
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOSTA)
                                            .setResposta(respostaLeilao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());

            Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipo(TipoNotificacao.CRIACAO)
                                            .setTipoMensagem(TipoAcao.LEILAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .setTempo(tempo)
                                            .build();

            String topic = "leilao::" + empresa + "::";
            socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
            socketNotificacoes.send(notificacao.toByteArray());

            return true;
        }
        else{
            Resposta respostaLeilao = Resposta.newBuilder()
                            .setTipo(TipoMensagem.LEILAO)
                            .setUtilizador(empresa)
                            .setSucesso(false)
                            .setMensagem("Leilão não foi criado com sucesso!")
                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                            .setTipo(TipoResposta.RESPOSTA)
                            .setResposta(respostaLeilao)
                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
            //manda mensagem a dizer q n foi criado
        }
        return false;
    }
    public synchronized boolean licitaLeilao(String empresa, long montante, float taxa, String investidor){
        try{
            Empresa aux = empresas.get(empresa);
            if(aux == null){
                return false;
            }
            boolean esta = aux.licitaLeilao(investidor, montante, taxa);
            String mensagem = "Licitacao para o leilao da empresa " + empresa;
            mensagem += ", com o montante " + montante + ", e a taxa " + taxa;
            
            if(esta){
                mensagem += " adicionada com sucesso!";
            }
            else{
                mensagem += " não foi considerada!";
            }
            //responde apenas ao cliente
            Resposta respostaLeilao = Resposta.newBuilder()
                                        .setTipo(TipoMensagem.LEILAO)
                                        .setUtilizador(investidor)
                                        .setSucesso(esta)
                                        .setMensagem(mensagem)
                                        .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOSTA)
                                            .setResposta(respostaLeilao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
            
            //tem de enviar notificacao
            if(esta){
                Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipo(TipoNotificacao.LICITACAO)
                                            .setTipoMensagem(TipoAcao.LEILAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .build();
                String topic = "leilao::" + empresa + "::";
                socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
                socketNotificacoes.send(notificacao.toByteArray());
            }
        
        }
        catch(ExcecaoFinalizado ef){
            //indica que já se encontra terminado
            Resposta respostaEmissao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.LEILAO)
                                            .setUtilizador(investidor)
                                            .setSucesso(false)
                                            .setMensagem("Para a empresa: " + ef.empresa + ": " + ef.mensagem)
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOSTA)
                                            .setResposta(respostaEmissao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
        }
        catch(ExcecaoUltrapassado eu){
            //responde ao cliente
            String mensagem = "Licitacao para o leilao da empresa " + empresa;
            mensagem += ", com o montante " + montante + ", e a taxa " + taxa;
            mensagem += " adicionada com sucesso!";
            //responde apenas ao cliente
            Resposta respostaLeilao = Resposta.newBuilder()
                                        .setTipo(TipoMensagem.LEILAO)
                                        .setUtilizador(investidor)
                                        .setSucesso(true)
                                        .setMensagem(mensagem)
                                        .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOSTA)
                                            .setResposta(respostaLeilao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());

            //manda para as notificacoes
            Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipo(TipoNotificacao.LICITACAO)
                                            .setTipoMensagem(TipoAcao.LEILAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .build();
            String topic = "leilao::" + empresa + "::";
            socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
            socketNotificacoes.send(notificacao.toByteArray());

            //manda aos que foram ultrapassados
            for(Proposta p : eu.propostas){
                NotificacaoUltrapassado nu = NotificacaoUltrapassado.newBuilder()
                                    .setEmpresa(eu.empresa)
                                    .setUtilizador(p.cliente)
                                    .setTaxa(p.taxa)
                                    .setValor(p.montante)
                                    .setTexto("A última proposta encontra-se como: " + eu.proposta.toString())
                                    .build();
                RespostaExchange notificacaoFinal = RespostaExchange.newBuilder()
                                    .setTipo(TipoResposta.NOTIFICACAO)
                                    .setNotificacao(nu)
                                    .build();
                socketExchangePush.send(notificacaoFinal.toByteArray());
            }
        }
        return true;
    }

    public synchronized long tempoDormir(){
        if(paraTerminar.size() == 0){
            return Long.MAX_VALUE;
        }

        return LocalDateTime.now().until(paraTerminar.first().emprestimo.fim,ChronoUnit.MILLIS);
    }

    public synchronized void termina(/*, o para comunicacao com o diretorio*/){
        //verifica todos os leiloes e termina os que já tiverem sido passados o tempo
        System.out.println("Terminar!!!");
        ArrayList<EmprestimoProntoTerminar> eliminar = new ArrayList<>();
        for(EmprestimoProntoTerminar ept: paraTerminar){
            if(ept.emprestimo.fim.compareTo(LocalDateTime.now()) > 0){
                break;
            }
            else{
                eliminar.add(ept);
                System.out.println(ept.emprestimo.empresa);
                Emprestimo emp = empresas.get(ept.emprestimo.empresa).terminaEmprestimo(ept.emprestimo.id);
                
                if(emp != null){
                    //mandar mensagens com o resultado

                    if(emp instanceof Leilao){
                        //indica que leilao terminou
                        Resultado resultado = Resultado.newBuilder()
                                        .setTipo(TipoMensagem.LEILAO)
                                        .setEmpresa(emp.empresa)
                                        .setTexto(emp.propostas.toString())
                                        .build();
                        RespostaExchange resultadoFinal = RespostaExchange.newBuilder()
                                        .setTipo(TipoResposta.RESULTADO)
                                        .setResultado(resultado)
                                        .build();
                        socketExchangePush.send(resultadoFinal.toByteArray());
                        //tem de enviar notificacao
                        ResultadoAcao resultadoA = ResultadoAcao.newBuilder()
                                        .setTipo(TipoAcao.LEILAO)
                                        .setTexto(emp.toString())
                                        .build();

                        Notificacao notificacao = Notificacao.newBuilder()
                                    .setTipo(TipoNotificacao.FIM)
                                    .setTipoMensagem(TipoAcao.LEILAO)
                                    .setEmpresa(emp.empresa)
                                    .setMontante(emp.montante)
                                    .setResultado(resultadoA)
                                    .build();

                        String topic = "leilao::" + emp.empresa + "::";
                        socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
                        socketNotificacoes.send(notificacao.toByteArray());

                        //tem de enviar para o diretorio


                    }
                    else{
                        //indica que emissao terminou
                        Resultado resultado = Resultado.newBuilder()
                                                        .setTipo(TipoMensagem.EMISSAO)
                                                        .setEmpresa(emp.empresa)
                                                        .setTexto(emp.propostas.toString())
                                                        .build();
                        RespostaExchange resultadoFinal = RespostaExchange.newBuilder()
                                                        .setTipo(TipoResposta.RESULTADO)
                                                        .setResultado(resultado)
                                                        .build();
                        socketExchangePush.send(resultadoFinal.toByteArray());
                        //tem de enviar notificacao
                        ResultadoAcao resultadoA = ResultadoAcao.newBuilder()
                                                        .setTipo(TipoAcao.EMISSAO)
                                                        .setTexto(emp.toString())
                                                        .build();

                        Notificacao notificacao = Notificacao.newBuilder()
                                                    .setTipo(TipoNotificacao.FIM)
                                                    .setTipoMensagem(TipoAcao.EMISSAO)
                                                    .setEmpresa(emp.empresa)
                                                    .setMontante(emp.montante)
                                                    .setResultado(resultadoA)
                                                    .build();

                        String topic = "emissao::" + emp.empresa + "::";
                        socketNotificacoes.send(topic.getBytes(),1/*ZMQ_SNDMORE*/);
                        socketNotificacoes.send(notificacao.toByteArray());
                            
                        //tem de enviar para o diretorio
                
                    }
                }
            }
        }

        paraTerminar.removeAll(eliminar);
        
    }
    
}