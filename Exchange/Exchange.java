import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.zeromq.ZMQ;

class EmprestimoProntoTerminar{
    public Emprestimo emprestimo;

    public EmprestimoProntoTerminar(Emprestimo e){
        emprestimo = e;
    }

    public int compareTo(EmprestimoProntoTerminar ept){
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

    public EstruturaExchange(ZMQ.context c){
        context = c;
        socketExchangePull = context.socket(ZMQ.PULL);
        socketNotificacoes = context.socket(ZMQ.PUB);
        socketExchangePush.bind("tcp://*:" + myPush);
        socketNotificacoes.bind("tcp://*:" + myPub);

    }
    private void possoInterromper(){
        acaba.interrupt();
    }
    
    //tempo em dias para minutos (1 dia corresponde a 1 minuto), devolve em nanos
    private long converteTempo(long tempo){
        return (long)((tempo*(10^9))/(24*60));
    }

    public synchronized boolean adicionaEmissao(String empresa, int montante, long tempo){
        LocalDateTime fim = LocalDateTime.now().plusNanos(converteTempo(tempo));
        Emprestimo aux = empresas.get(empresa).criarEmissao(montante,fim);
        if(aux != null){
            EmprestimoProntoTerminar ept = new EmprestimoProntoTerminar(aux);
            if(ept.equals(paraTerminar.first())){
                possoInterromper();
            }
            //manda para o diretorio
            Resposta respostaEmissao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.EMISSAO)
                                            .setUtilizador(empresa)
                                            .setSucesso(true)
                                            .setMensagem("Emissao criada com sucesso!")
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOTA)
                                            .setResposta(respostaEmissao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());

            Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipoNotificacao(TipoNotificacao.CRIACAOEMISSAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .setTempo(tempo)
                                            .build();

            String topic = "emissao::" + empresa + "::";
            socketNotificacoes.send(topic.getBytes,ZMQ_SNDMORE);
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
                            .setTipo(TipoResposta.RESPOTA)
                            .setResposta(respostaEmissao)
                            .build();
        socketExchangePush.send(respostaFinal.toByteArray());

        return false;
    }

    public synchronized boolean licitaEmissao(String empresa, int montante, String investidor){
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
                socketExchangePush(resultadoFinal.toByteArray());
                //tem de enviar notificacao
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
                                                .setTipo(TipoResposta.RESPOTA)
                                                .setResposta(respostaEmissao)
                                                .build();
                socketExchangePush.send(respostaFinal.toByteArray());
                
                //tem de enviar notificacao
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
                                            .setTipo(TipoResposta.RESPOTA)
                                            .setResposta(respostaEmissao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
        }
    }

    public synchronized boolean adicionaLeilao(String empresa, int montante, float taxa, long tempo){
        LocalDateTime fim = LocalDateTime.now().plusNanos(converteTempo(tempo));
        Emprestimo aux = empresas.get(empresa).criarLeilao(montante, taxa, fim);
        if(aux != null){
            EmprestimoProntoTerminar ept = new EmprestimoProntoTerminar(aux);
            if(ept.equals(paraTerminar.first())){
                possoInterromper();
            }
            //manda para o diretorio
            Resposta respostaLeilao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.LEILAO)
                                            .setUtilizador(empresa)
                                            .setSucesso(true)
                                            .setMensagem("Emissao criada com sucesso!")
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOTA)
                                            .setResposta(respostaLeilao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());

            Notificacao notificacao = Notificacao.newBuilder()
                                            .setTipoNotificacao(TipoNotificacao.CRIACAOLEILAO)
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .setTempo(tempo)
                                            .build();

            String topic = "emissao::" + empresa + "::";
            socketNotificacoes.send(topic.getBytes,ZMQ_SNDMORE);
            socketNotificacoes.send(notificacao.toByteArray());

            return true;
        }
        else{
            Resposta respostaLeilao = Resposta.newBuilder()
                            .setTipo(TipoMensagem.LEILAO)
                            .setUtilizador(empresa)
                            .setSucesso(false)
                            .setMensagem("Emissao não foi criada com sucesso!")
                            .build();
        RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                            .setTipo(TipoResposta.RESPOTA)
                            .setResposta(respostaLeilao)
                            .build();
        socketExchangePush.send(respostaFinal.toByteArray());
            //manda mensagem a dizer q n foi criado
        }
        return false;
    }
    public synchronized boolean licitaLeilao(String empresa, int montante, float taxa, String investidor){
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
                                            .setTipo(TipoResposta.RESPOTA)
                                            .setResposta(respostaLeilao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());
            
            //tem de enviar notificacao
        
        }
        catch(ExcecaoFinalizado ef){
            //indica que já se encontra terminado
            Resposta respostaEmissao = Resposta.newBuilder()
                                            .setTipo(TipoMensagem.LEILAO)
                                            .setUtilizador(investidor)
                                            .setSucesso(true)
                                            .setMensagem("Para a empresa: " + ef.empresa + ": " + ef.mensagem)
                                            .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOTA)
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
                                        .setSucesso(esta)
                                        .setMensagem(mensagem)
                                        .build();
            RespostaExchange respostaFinal = RespostaExchange.newBuilder()
                                            .setTipo(TipoResposta.RESPOTA)
                                            .setResposta(respostaLeilao)
                                            .build();
            socketExchangePush.send(respostaFinal.toByteArray());

            //manda para as notificacoes

            //manda aos que foram ultrapassados
            for(Proposta p : eu.propostas){
                NotificacaoUltrapassado nu = NotificacaoUltrapassado.newBuilder()
                                    .setEmpresa(eu.empresa)
                                    .setUtilizador(p.cliente)
                                    .setTaxa(p.taxa)
                                    .setValor(p.montante)
                                    .setTexto("A última proposta encontra-se como: " + nu.proposta.toString())
                                    .build();
                RespostaExchange notificacaoFinal = RespostaExchange.newBuilder()
                                    .setTipo(TipoResposta.Notificacao)
                                    .setNotificacao(nu)
                                    .build();
                socketExchangePush.send(notificacaoFinal.toByteArray());
            }
        }
    }

    public synchronized long tempoDormir(){
        if(paraTerminar.size() == 0){
            return Long.MAX_VALUE;
        }

        return LocalDateTime.now().until(paraTerminar.first(),ChronoUnit.MILLIS);
    }

    public synchronized void termina(/*, o para comunicacao com o diretorio*/){
        //verifica todos os leiloes e termina os que já tiverem sido passados o tempo
        ArrayList<EmprestimoProntoTerminar> eliminar = new ArrayList<>();
        for(EmprestimoProntoTerminar ept: paraTerminar){
            if(ept.emprestimo.fim.compareTo(LocalDateTime.now()) > 0){
                break;
            }
            else{
                eliminar.add(ept);
                Emprestimo emp = empresas.get(ept.emprestimo.empresa).terminaEmprestimo(ept.emprestimo.id);
                
                if(emp != null){
                    //mandar mensagens com o resultado
                }
            }
        }

        paraTerminar.removeAll(eliminar);
        
    }
    
}
class TerminaEmprestimo implements Runnable{
    //ZMQ.Context context;
    //ZMQ.Socket socketExchangePush;
    //ZMQ.Socket socketNotificacoes;    
    //falta o de comunicacao com o diretorio

    //faltam as proximas a serem terminadas
    EstruturaExchange estrutura;

    public TerminaEmprestimo(ZMQ.Context c, ZMQ.Socket p, ZMQ.Socket n/*,falta o de comunicacao 
    com o diretorio */, EstruturaExchange ee){
        context = c;
        socketExchangePush = p;
        socketNotificacoes = n;
        estrutura = ee;
    }

    public void run(){
        //verifica se existem terminadas e caso existam verifica se ja terminaram
        //senao dorme até uma terminar
        while(true){
            
            try{
                long tempoDormir = estrutura.tempoDormir();
                Thread.sleep(tempoDormir);
                estrutura.termina(socketExchangePush,socketNotificacoes);
            }
            catch(InterruptedException ie){
                System.out.println("Fui interrompido!");
            }
            
        }
    }
}

public class Exchange{
    Thread acaba = null;
    ZMQ.Context context = ZMQ.context(1);
    EstruturaExchange estrutura = new EstruturaExchange(context);
    ZMQ.Socket socketExchangePull = context.socket(ZMQ.PULL);
    //ZMQ.Socket socketExchangePush = context.socket(ZMQ.PUSH);
    //ZMQ.Socket socketNotificacoes = context.socket(ZMQ.PUB);
    //falta o socket de comunicacao com o diretorio

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    public static void main(String[] args){
        //carrega de alguma forma as empresas
        //carrega de alguma forma os seus enderecos e portas
        //carrega de alguma forma os enderecos e portas do diretorio e exchange
        socketExchangePull.bind("tcp://*:" + myPull);
        acaba = new Thread(new TerminaEmprestimo(context, socketExchangePush, socketNotificacoes, estrutura));
        acaba.start();
        estrutura.acaba = acaba;

        while(true){
            byte[] bResposta = socketExchangePull.recv();
            MensagemUtilizador resposta = MensagemUtilizador.parseFrom(bResposta);

            //verifica tipo da mensagem
            if(resposta.hasEmpresa()){
                if(resposta.getTipoMensagem() == TipoMensagem.LEILAO){
                    estrutura.adicionaLeilao(resposta.getUtilizador(),
                        resposta.getEmpresa().getEmissaoTaxaFixa().getMontante(),
                        resposta.getEmpresa().getEmissaoTaxaFixa().getTaxa(),
                        resposta.getEmpresa().getEmissaoTaxaFixa().getTempo());
                }
                else{
                    if(resposta.getTipoMensagem() == TipoMensagem.EMISSAO){
                        estrutura.adicionaEmissao(resposta.getUtilizador(),
                            resposta.getEmpresa().getEmissaoTaxaFixa().getMontante(),
                            resposta.getEmpresa().getEmissaoTaxaFixa().getTempo());
                    }
                    else{
                        //erro
                    }
                }
            }
            else{
                if(resposta.hasInvestidor()){
                    if(resposta.getTipoMensagem() == TipoMensagem.LEILAO){
                        estrutura.licitaLeilao(resposta.getUtilizador(),
                            resposta.getInvestidor().getLeilao().getMontante(),
                            resposta.getInvestidor().getLeilao().getTaxa(),
                            resposta.getUtilizador());
                    }
                    else{
                        if(resposta.getTipoMensagem() == TipoMensagem.EMISSAO){
                            estrutura.licitaLeilao(resposta.getUtilizador(),
                                resposta.getInvestidor().getLeilao().getMontante(),
                                resposta.getUtilizador());
                        }
                        else{
                            //erro
                        }
                    }
                }
            }
        }
    }
    
}