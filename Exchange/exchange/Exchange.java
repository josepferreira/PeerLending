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

import org.zeromq.ZMQ;

import exchange.Ccs.*;

class TerminaEmprestimo implements Runnable{
    //ZMQ.Context context;
    //ZMQ.Socket socketExchangePush;
    //ZMQ.Socket socketNotificacoes;    
    //falta o de comunicacao com o diretorio

    //faltam as proximas a serem terminadas
    EstruturaExchange estrutura;

    public TerminaEmprestimo(EstruturaExchange ee){
        
        estrutura = ee;
    }

    public void run(){
        //verifica se existem terminadas e caso existam verifica se ja terminaram
        //senao dorme até uma terminar
        while(true){
            
            try{
                long tempoDormir = estrutura.tempoDormir();
                Thread.sleep(tempoDormir);
                estrutura.termina();
            }
            catch(InterruptedException ie){
                System.out.println("Fui interrompido!");
            }
            
        }
    }
}

public class Exchange{
    //ZMQ.Socket socketExchangePush = context.socket(ZMQ.PUSH);
    //ZMQ.Socket socketNotificacoes = context.socket(ZMQ.PUB);
    //falta o socket de comunicacao com o diretorio

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    public static void main(String[] args){
        System.out.println("====================Chegou ao exchange=====================");
        //carrega de alguma forma as empresas
        //carrega de alguma forma os seus enderecos e portas
        //carrega de alguma forma os enderecos e portas do diretorio e exchange
        ZMQ.Context context = ZMQ.context(1);
        EstruturaExchange estrutura = new EstruturaExchange(context, "12350", "12352");
        ZMQ.Socket socketExchangePull = context.socket(ZMQ.PULL);
        String myPull = "12351";
    
        socketExchangePull.bind("tcp://*:" + myPull);
        Thread acaba = new Thread(new TerminaEmprestimo(estrutura));
        acaba.start();
        estrutura.acaba = acaba;

        while(true){
            byte[] bResposta = socketExchangePull.recv();
            try{
                MensagemUtilizador resposta = MensagemUtilizador.parseFrom(bResposta);

                //verifica tipo da mensagem
                if(resposta.getTipoUtilizador() == TipoUtilizador.EMPRESA){
                    if(resposta.getTipo() == TipoMensagem.LEILAO){
                        estrutura.adicionaLeilao(resposta.getUtilizador(),
                            resposta.getEmpresa().getLeilao().getMontante(),
                            resposta.getEmpresa().getLeilao().getTaxa(),
                            resposta.getEmpresa().getLeilao().getTempo());
                    }
                    else{
                        if(resposta.getTipo() == TipoMensagem.EMISSAO){
                            estrutura.adicionaEmissao(resposta.getUtilizador(),
                                resposta.getEmpresa().getEmissao().getMontante(),
                                resposta.getEmpresa().getEmissao().getTempo());
                        }
                        else{
                            //erro
                        }
                    }
                }
                else{
                    if(resposta.getTipoUtilizador() == TipoUtilizador.INVESTIDOR){
                        if(resposta.getTipo() == TipoMensagem.LEILAO){
                            estrutura.licitaLeilao(resposta.getInvestidor().getLeilao().getEmpresa(),
                                resposta.getInvestidor().getLeilao().getMontante(),
                                resposta.getInvestidor().getLeilao().getTaxa(),
                                resposta.getUtilizador());
                        }
                        else{
                            if(resposta.getTipo() == TipoMensagem.EMISSAO){
                                estrutura.licitaEmissao(resposta.getInvestidor().getEmissao().getEmpresa(),
                                    resposta.getInvestidor().getEmissao().getMontante(),
                                    resposta.getUtilizador());
                            }
                            else{
                                //erro
                            }
                        }
                    }
                }
            }
            catch(Exception exc){
                System.out.println(exc);
            }
        }
    }
    
}