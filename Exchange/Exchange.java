import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.TreeSet;

import org.zeromq.ZMQ;

class EmprestimoProntoTerminar{
    public Emprestimo emprestimo;

    public EmprestimoProntoTerminar(Emprestimo e){
        emprestimo = e;
    }

    public int compareTo(EmprestimoProntoTerminar ept){
        return this.emprestimo.fim.compareTo(ept.emprestimo.fim);
    }
}
class EstruturaExchange{
    public TreeSet<EmprestimoProntoTerminar> paraTerminar = new TreeSet<>();
    public HashMap<String,Empresa> empresas = new HashMap<>();
    
    public long tempoDormir(){
        if(paraTerminar.size() == 0){
            return 1000000;
        }

        return LocalDateTime.now().until(paraTerminar.first(),TemporalUnit.MILLIS);
    }

    public boolean possoTerminar(EmprestimoProntoTerminar ept){
        return (ept.emprestimo.fim.compareTo(LocalDateTime.now()) < 1);
    }
    public void termina(){
            //verifica todos os leiloes e termina os que já tiverem sido passados o tempo
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
            long tempoDormir = estrutura.tempoDormir();
            try{
                Thread.sleep(tempoDormir);
            }
            catch(InterruptedException ie){
                System.out.println("Fui interrompido!");
            }
            estrutura.termina();
        }
    }
}

public class Exchange{
    EstruturaExchange estrutura = new EstruturaExchange();
    Thread acaba = null;
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket socketExchangePull = context.socket(ZMQ.PULL);
    ZMQ.Socket socketExchangePull = context.socket(ZMQ.PUSH);
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
                    //responde ao cliente do sucesso pelo exchange
                    //envia licitacao para o sistema de notificacoes
                    if(/*existem ultrapassados*/){
                        //avisa os ultrapassados pelo exchange
                    }
                }
                else{
                    //responde ao cliente do insucesso pelo exchange
                }
                if(/*emissao terminou*/){
                    //responde pelo exchange q terminou
                    //manda ao diretorio
                    //manda para as notificacoes
                }
            }
        }
    }
    
}