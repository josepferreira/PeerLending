package exchange;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;


import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.zeromq.ZMQ;

import exchange.Ccs.*;
import exchange.NotificacaoOuterClass.*;


class EstruturaExchange{
    public TreeSet<Emprestimo> paraTerminar = new TreeSet<Emprestimo>((o1, o2) -> {

        Emprestimo e1 = (Emprestimo) o1;
        Emprestimo e2 = (Emprestimo) o2;

        return e1.fim.compareTo(e2.fim);
    });

    public HashMap<String,Empresa> empresas = new HashMap<>();
    public ZMQ.Context context;
    public ZMQ.Socket socketExchangePush;
    public ZMQ.Socket socketNotificacoes;
    public String urlDiretorio;
    
    //so falta o diretorio

    public EstruturaExchange(ZMQ.Context c, String myPush, String myPub, 
                    ArrayList<String> empresas, String endDir, String portaDir){
        context = c;
        socketNotificacoes = context.socket(ZMQ.PUB);
        socketExchangePush = context.socket(ZMQ.PUSH);
        socketExchangePush.bind("tcp://*:" + myPush);
        socketNotificacoes.bind("tcp://*:" + myPub);
        urlDiretorio  = "http://" + endDir + ":" + portaDir + "/";
        
        for(String emp: empresas){
            this.empresas.put(emp, new Empresa(emp));
        }

        System.out.println("Estrutura configurada");

    }
    
    //tempo: 1 hora -> 12 segundos : devolve em nanos
    private long converteTempo(long tempo){
        
        return (long)(((tempo*Math.pow(10, 9)*12)));
    }

    //cria a thread para acabar com o leilao
    private void criaThread(Emprestimo emp){
        new Thread(new TerminaEmprestimo(emp.fim, context)).start();
    }

    public boolean adicionaEmissao(String empresa, long montante, long tempo){
        long tempoAux = converteTempo(tempo);
        System.out.println("TEMPO: " + tempoAux);
        System.out.println("EMpresa: " + empresa);
        LocalDateTime fim = LocalDateTime.now().plusNanos(tempoAux);
        Emprestimo aux = null;
        try{
            aux = empresas.get(empresa).criarEmissao(montante,fim);
        }
        catch(ExcecaoIndisponivel ei){
            Resposta respostaEmissao = Resposta.newBuilder()
                            .setTipo(TipoMensagem.EMISSAO)
                            .setUtilizador(empresa)
                            .setSucesso(false)
                            .setMensagem(ei.mensagem)
                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                            .setTipo(TipoResposta.RESPOSTA)
                            .setResposta(respostaEmissao)
                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
            return false;
        }

        if(aux != null){
            paraTerminar.add(aux);
            criaThread(aux);
            System.out.println("Adicionei: " + aux.empresa);
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
            socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
            socketNotificacoes.send(notificacao.toByteArray());

            try{
                URL url = new URL(urlDiretorio + "emissao");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setInstanceFollowRedirects(false);
                con.setUseCaches(false);
    
                con.connect();
                OutputStream out = con.getOutputStream();
                out.write(((Emissao)aux).getJSON().getBytes());
                out.flush();
                out.close();
    
                System.out.println(((Emissao)aux).getJSON());
        
                int responseCode = con.getResponseCode();
                System.out.println("POST emissao Response Code :: " + responseCode);
    
            }catch(Exception exc){
                System.out.println(exc);
            }

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

    public boolean licitaEmissao(String empresa, long montante, String investidor){
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
                socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
                socketNotificacoes.send(notificacao.toByteArray());
                    
                //tem de enviar para o diretorio
                try{
                    URL url = new URL(urlDiretorio + "emissao/" + em.empresa
                                    + "/terminado/" + em.id);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("PUT");
                    con.setDoOutput(true);
                            con.setRequestProperty("Content-Type", "application/json");
                            con.setInstanceFollowRedirects(false);
                            con.setUseCaches(false);
                
                            con.connect();
                            OutputStream out = con.getOutputStream();
                            out.write(em.getJSON().getBytes());
                            out.flush();
                            out.close();
                    
                    int responseCode = con.getResponseCode();
                    System.out.println("PUT emissao Response Code :: " + responseCode);
        
                }catch(Exception exc){
                    System.out.println(exc);
                }
                System.out.println("PAssei terminar emissao!");
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
                socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
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

    public boolean adicionaLeilao(String empresa, long montante, float taxa, long tempo){
        LocalDateTime fim = LocalDateTime.now().plusNanos(converteTempo(tempo));
        Emprestimo aux = empresas.get(empresa).criarLeilao(montante, taxa, fim);
        if(aux != null){
            paraTerminar.add(aux);
            criaThread(aux);
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
            socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
            socketNotificacoes.send(notificacao.toByteArray());


            try{
                URL url = new URL(urlDiretorio + "leilao");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setInstanceFollowRedirects(false);
                con.setUseCaches(false);

                con.connect();
                OutputStream out = con.getOutputStream();
                out.write(((Leilao)aux).getJSON().getBytes());
                out.flush();
                out.close();

                System.out.println(((Leilao)aux).getJSON());
		
                int responseCode = con.getResponseCode();
                System.out.println("POST leilao Response Code :: " + responseCode);

            }catch(Exception exc){
                System.out.println(exc);
            }
        
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
    public boolean licitaLeilao(String empresa, long montante, float taxa, String investidor){
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
                socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
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
            socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
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

    public void termina(/*, o para comunicacao com o diretorio*/){
        //verifica todos os leiloes e termina os que já tiverem sido passados o tempo
        System.out.println("Terminar!!!");
        ArrayList<Emprestimo> eliminar = new ArrayList<>();
        for(Emprestimo ept: paraTerminar){
            if(ept.fim.compareTo(LocalDateTime.now()) > 0){
                break;
            }
            else{
                eliminar.add(ept);
                System.out.println(ept.empresa);
                Emprestimo emp = empresas.get(ept.empresa).terminaEmprestimo(ept.id);
                
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
                        socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
                        socketNotificacoes.send(notificacao.toByteArray());

                        
                        try{
                            URL url = new URL(urlDiretorio + "leilao/" + emp.empresa
                                            + "/terminado/" + emp.id);
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            con.setRequestMethod("PUT");
                            con.setDoOutput(true);
                            con.setRequestProperty("Content-Type", "application/json");
                            con.setInstanceFollowRedirects(false);
                            con.setUseCaches(false);
                
                            con.connect();
                            OutputStream out = con.getOutputStream();
                            out.write(((Leilao)emp).getJSON().getBytes());
                            out.flush();
                            out.close();
                
                            
                            int responseCode = con.getResponseCode();
                            System.out.println("PUT leilao Response Code :: " + responseCode);
                
                        }catch(Exception exc){
                            System.out.println(exc);
                        }

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
                        socketNotificacoes.send(topic.getBytes(), ZMQ.SNDMORE);
                        socketNotificacoes.send(notificacao.toByteArray());
                            
                        //tem de enviar para o diretorio
                        try{
                            URL url = new URL(urlDiretorio + "emissao/" + emp.empresa
                                            + "/terminado/" + emp.id);
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            con.setRequestMethod("PUT");
                            con.setDoOutput(true);
                            con.setRequestProperty("Content-Type", "application/json");
                            con.setInstanceFollowRedirects(false);
                            con.setUseCaches(false);
                
                            con.connect();
                            OutputStream out = con.getOutputStream();
                            out.write(((Emissao)emp).getJSON().getBytes());
                            out.flush();
                            out.close();
                
                            
                            int responseCode = con.getResponseCode();
                            System.out.println("PUT emissao Response Code :: " + responseCode);
                
                        }catch(Exception exc){
                            System.out.println(exc);
                        }
                
                    }
                }
            }
        }

        paraTerminar.removeAll(eliminar);
        
    }
    
}