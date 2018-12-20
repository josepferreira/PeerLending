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
            return true;
        }
        return false;
    }

    public synchronized boolean adicionaLeilao(String empresa, int montante, float taxa, long tempo){
        LocalDateTime fim = LocalDateTime.now().plusNanos(converteTempo(tempo));
        Emprestimo aux = empresas.get(empresa).criarLeilao(montante, taxa, fim);
        if(aux != null){
            EmprestimoProntoTerminar ept = new EmprestimoProntoTerminar(aux);
            if(ept.equals(paraTerminar.first())){
                possoInterromper();
            }
            return true;
        }
        return false;
    }

    public synchronized long tempoDormir(){
        if(paraTerminar.size() == 0){
            return Long.MAX_VALUE;
        }

        return LocalDateTime.now().until(paraTerminar.first(),ChronoUnit.MILLIS);
    }

    public synchronized void termina(ZMQ.Socket sP, ZMQ.Socket sN /*, o para comunicacao com o diretorio*/){
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
                    //mandar mensagens
                }
            }
        }

        paraTerminar.removeAll(eliminar);
        
    }
    
}
class TerminaEmprestimo implements Runnable{
    ZMQ.Context context;
    ZMQ.Socket socketExchangePush;
    ZMQ.Socket socketNotificacoes;    
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
    EstruturaExchange estrutura = new EstruturaExchange();
    Thread acaba = null;
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket socketExchangePull = context.socket(ZMQ.PULL);
    ZMQ.Socket socketExchangePush = context.socket(ZMQ.PUSH);
    ZMQ.Socket socketNotificacoes = context.socket(ZMQ.PUB);
    //falta o socket de comunicacao com o diretorio

    public static void main(String[] args){
        //carrega de alguma forma as empresas
        //carrega de alguma forma os seus enderecos e portas
        //carrega de alguma forma os enderecos e portas do diretorio e exchange
        socketExchangePull.bind("tcp://*:" + myPull);
        socketExchangePush.bind("tcp://*:" + myPush);
        socketNotificacoes.bind("tcp://*:" + myPub);
        acaba = new Thread(new TerminaEmprestimo(context, socketExchangePush, socketNotificacoes, estrutura));
        acaba.start();
        estrutura.acaba = acaba;

        while(true){
            socketExchangePull.recv();
            
            //verifica tipo da mensagem

            if(/*tipo da mensagem é empresa*/){
                //cria leilao ou emissao

                if(/*emprestimo criado*/){
                    //manda para o diretorio
                    //manda para o sistema de notificacoes
                    //tem de interromper a outra thread
                }
                //responde à empresa
            }
            else{
                //é do investidor
                
                //licita leilao ou empresa
                if(/*licitacao com sucesso*/){
                    //responde ao cliente do sucesso pelo front-end
                    //envia licitacao para o sistema de notificacoes
                    if(/*existem ultrapassados*/){
                        //avisa os ultrapassados pelo front-end
                    }
                }
                else{
                    //responde ao cliente do insucesso pelo front-end
                }
                if(/*emissao terminou*/){
                    //responde pelo front-end q terminou
                    //manda ao diretorio
                    //manda para as notificacoes
                }
            }
        }
    }
    
}