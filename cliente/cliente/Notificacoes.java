package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.zeromq.ZMQ;

import cliente.Ccs.*;
import cliente.NotificacaoOuterClass.*;

/**
 * Vou ter de ter duas threads:
 * 1) Para comunicar com o Cliente (inproc)
 *      Tratará de adicionar uma subscrição ... 
 * 2) Para comunicar com o Exchange
 */

class ComunicaCliente implements Runnable{

    private ZMQ.Context context;
    private ZMQ.Socket sub;
    private GerirSubscricoes subscricao;

    public ComunicaCliente(ZMQ.Context context, ZMQ.Socket sub, GerirSubscricoes subscricao){
        this.context = context;
        this.sub = sub;
        this.subscricao = subscricao;
    }

    public void run(){
        ZMQ.Socket socket = context.socket(ZMQ.PULL);
        socket.connect("inproc://notificacoes");

        //Vou ficar a escuta de algum pedido de subscrição
        while(!Thread.interrupted()){
            byte[] b = socket.recv();
            String msgResposta = new String(b);

            /**
             * Tenho de fazer parse à mensagem
             * Se for sub@ -> ENtao quer dizer que é para subscrever
             * Se for unsub@ -> então é para tirar o subscribe
             */


            String decisao = msgResposta.split("@")[0];
            String sub = msgResposta.split("@")[1];
            System.out.println("A decisao é " + decisao + ". A subscrição: " + sub);
            /**
             * Vou adicionar a subscricao à classe subscricao e ao socket
             * ATENÇAO!!! NAO SEI SE POSSO FAZER ISTO ..
             */
            if(decisao.equals("sub")){
                switch(sub){
                    case "leilao::": subscricao.leiloesSubscritos = true;  break;
                    case "emissao::": subscricao.emissoesSubscritas = true; break;
                    default: String empresa = sub.split("::")[1]; subscricao.adicionaEmpresa(empresa); break;
                }
                sub.subscribe(sub.getBytes());
            }else{
                if(decisao.equals("unsub")){
                    switch(sub){
                        case "leilao::": subscricao.leiloesSubscritos = false;  break;
                        case "emissao::": subscricao.emissoesSubscritas = false; break;
                        default: String empresa = sub.split("::")[1]; subscricao.removeEmpresa(empresa); break;
                    }
                    sub.unsubscribe(sub.getBytes());
                }else{
                    System.out.println("ERRO: Não vai fazer sub nem unsub!");
                }
            }
        }
    }

}scoket.connect("tcp://*:12352");

public class Notificacoes implements Runnable{

    GerirSubscricoes sub;
    ZMQ.Context context;

    public Notificacoes(ZMQ.Context context, GerirSubscricoes gb){
        this.context = context;
        this.sub = gb;
    }

    public void run(){
        //Vou ter de me associar às exchanges
        ZMQ.Socket socket = context.socket(ZMQ.SUB);
        socket.connect("tcp://*:12352");
        /**
         * Agora é necessário ter os IPs e portas ... Vou buscar ao diretorio?
         * socket.connect("tcp://ip:"+porta);
         * ip -> endereço da exchange
         * porta -> porta da exchange
         */

        ComunicaCliente cc = new ComunicaCliente(context, socket, sub);
        (new Thread(cc)).start();
        
        while(!Thread.interrupted()){
            byte[] b = socket.recv();
            /**
             * Aqui vou ter de fazer parse da mensagem 
             * Os primeiros bytes são de subscrição e os seguintes é a mensagem de subscrição
             */

            byte[] mensagem = null;
            try{
                Notificacao notificacao = Notificacao.parseFrom(mensagem);
                String head = null;

                if(notificacao.getTipo() == TipoNotificacao.CRIACAOLEILAO){
                    head = "Foi criado um leilão pela empresa ";
                }else{
                    if(notificacao.getTipo() == TipoNotificacao.LICITACAOLEILAO){
                        head = "Foi acrescentada uma licitação ao leilão da empresa ";
                    }else{
                        if(notificacao.getTipo() == TipoNotificacao.CRIACOEMISSAO){
                            head = "Foi criada uma emissão pela empresa ";
                        }else{
                            if(notificacao.getTipo() == TipoNotificacao.LICITACAOEMISSAO){
                                head = "Foi acrescentada uma subscrição à emissão da empresa ";
                            }
                        }
                    }
                }

                /*switch(notificacao.getTipo()){
                    case TipoNotificacao.CRIACAOLEILAO: head = "Foi criado um leilão pela empresa "; break;
                    case TipoNotificacao.LICITACAOLEILAO: head = "Foi acrescentada uma licitação ao leilão da empresa "; break;
                    case TipoNotificacao.CRIACOEMISSAO: head = "Foi criada uma emissão pela empresa "; break;
                    case TipoNotificacao.LICITACAOEMISSAO: head = "Foi acrescentada uma subscrição à emissão da empresa "; break;
                }*/

                String msg = head + notificacao.getEmpresa() + " com o montante " + notificacao.getMontante() + " e uma taxa de " + notificacao.getTaxa();
                System.out.println(msg);
            }catch(Exception e){
                System.out.println("ERRO: Deu um erro a fazer parse da NOtificacao");
            }
        }



    }

}