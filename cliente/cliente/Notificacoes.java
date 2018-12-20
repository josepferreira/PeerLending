package cliente;
import org.zeromq.ZMQ;
import java.util.HashSet;

/**
 * Vou ter de ter duas threads:
 * 1) Para comunicar com o Cliente (inproc)
 *      Tratará de adicionar uma subscrição ... 
 * 2) Para comunicar com o Exchange
 */

class Subscricao{

    boolean leiloesSubscritos = false;
    boolean emissoesSubscritas = false;
    HashSet<String> empresasSubscritas = new HashSet<>();

    public void adicionaEmpresa(String empresa){
        empresasSubscritas.add(empresa);
    }

    public void removeEmpresa(String empresa){
        empresasSubscritas.remove(empresa);
    }

}

class ComunicaCliente implements Runnable{

    private ZMQ.Context context;
    private ZMQ.Sokcet sub;
    private Subscricao subscricao;

    public ComunicaCliente(ZMQ.Context context, ZMQ.Socket sub, Subscricao subscricao){
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

            /**
             * Vou adicionar a subscricao à classe subscricao e ao socket
             * ATENÇAO!!! NAO SEI SE POSSO FAZER ISTO ..
             */
            if(decisao.equals("sub")){
                switch(sub){
                    case "leilao": subscricao.leiloesSubscritos = true;  break;
                    case "emissao": subscricao.emissoesSubscritas = true; break;
                    default: subscricao.adicionaEmpresa(sub); break;
                }
                socket.subscribe(sub.getBytes());
            }else{
                if(decisao.equals("unsub")){
                    switch(sub){
                        case "leilao": subscricao.leiloesSubscritos = false;  break;
                        case "emissao": subscricao.emissoesSubscritas = false; break;
                        default: subscricao.removeEmpresa(sub); break;
                    }
                    socket.unsubscribe(sub.getBytes());
                }else{
                    System.out.println("ERRO: Não vai fazer sub nem unsub!");
                }
            }
        }
    }

}

public class Notificacoes implements Runnable{

    Subscricaco sub = new Subscricao();

    public Notificacoes(){}

    public void run(){
        //Vou ter de me associar às exchanges
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.SUB);
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
            Notificacao notificacao = mensagem.parseFrom(mensagem);
            String head = null;
            switch(notificacao.getTipo()){
                case TipoNotificacao.CRIACAOLEILAO: head = "Foi criado um leilão pela empresa "; break;
                case TipoNotificacao.LICITACAOLEILAO: head = "Foi acrescentada uma licitação ao leilão da empresa "; break;
                case TipoNotificacao.CRIACOEMISSAO: head = "Foi criada uma emissão pela empresa "; break;
                case TipoNotificacao.LICITACAOEMISSAO: head = "Foi acrescentada uma subscrição à emissão da empresa "; break;
            }

            String msg = head + notificacao.getEmpresa() + " com o montante " + notificacao.getMontante() + " e uma taxa de " + notificacao.getTaxa();
            System.out.println(msg);
        }



    }

}