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
    private HashMap<String, ArrayList<String>> enderecos = new HashMap<>();
    private boolean temTodos = false;

    public ComunicaCliente(ZMQ.Context context, ZMQ.Socket sub, GerirSubscricoes subscricao){
        this.context = context;
        this.sub = sub;
        this.subscricao = subscricao;
    }

    private boolean jaTemEndereco(String empresa){
        for(ArrayList<String> lista: enderecos.values()){
            if(lista.contains(empresa))
                return true;
        }

        //Quer dizer que não encontrou a empresa!
        return false;
    }

    private void registaTodosEnderecos(){
        System.out.println("Vou fazer connect a todos!");
        this.sub.connect("tcp://*:12352");
        /*try{
            URL url = new URL("http://localhost:8080/exchange/todas");
            System.out.println("VOU EVNIAR UM EPDIDO!");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
		    in.close();

		    //print result
            System.out.println(response.toString());
            JSONArray jsonArray = new JSONArray(response.toString());
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject exchange = jsonArray.getJSONObject(i);
                String end = exchange.endereco;
                ArrayList<String> empresas = exchange.empresas;
                enderecos.put(end, empresas);
                this.sub.connect("tcp://"+end);
            }
            temTodos = true;

        }catch(Exception exc){
            System.out.println(exc);
        }*/
    }

    private void registaNovoEndereco(String empresa){
        System.out.println("Vou subscrever a empresa " + empresa);
        this.sub.connect("tcp://*:12352");

        /*try{
            URL url = new URL("http://localhost:8080/exchange?empresa="+empresa);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
		    in.close();

		    //print result
            System.out.println(response.toString());
            JSONObject exchange = new JSONObject(response.toString());
            String end = exchange.endereco;
            ArrayList<String> empresas = exchange.empresas;
            enderecos.put(end, empresas);
            this.sub.connect("tcp://"+end);

        }catch(Exception exc){
            System.out.println(exc);
        }*/

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
            String subResposta = msgResposta.split("@")[1];
            System.out.println("\nA decisao é " + decisao + ". A subscrição: " + subResposta);
            /**
             * Vou adicionar a subscricao à classe subscricao e ao socket
             * ATENÇAO!!! NAO SEI SE POSSO FAZER ISTO ..
             */
            if(decisao.equals("sub")){
                switch(subResposta){
                    case "leilao::": 
                    subscricao.leiloesSubscritos = true;
                    if(!temTodos)
                        registaTodosEnderecos();  
                    break;
                    case "emissao::": 
                        subscricao.emissoesSubscritas = true; 
                        if(!temTodos)
                            registaTodosEnderecos();
                        break;
                    default: 
                        String empresa = subResposta.split("::")[1]; 
                        subscricao.adicionaEmpresa(empresa);
                        if(!jaTemEndereco(empresa)){
                            registaNovoEndereco(empresa);
                        } 
                        break;
                }
                sub.subscribe(subResposta.getBytes());
                System.out.println("Ja subscrevi!!");
            }else{
                if(decisao.equals("unsub")){
                    switch(subResposta){
                        case "leilao::": subscricao.leiloesSubscritos = false;  break;
                        case "emissao::": subscricao.emissoesSubscritas = false; break;
                        default: 
                            String empresa = subResposta.split("::")[1]; 
                            subscricao.removeEmpresa(empresa); 
                            break;
                    }
                    sub.unsubscribe(subResposta.getBytes());
                    System.out.println("Ja tirei a subscricao!!");
                }else{
                    System.out.println("ERRO: Não vai fazer sub nem unsub!");
                }
            }
        }
    }

}

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
        //socket.connect("inproc://enderecos");
        //socket.connect("tcp://*:12352");
        /**
         * Agora é necessário ter os IPs e portas ... Vou buscar ao diretorio?
         * socket.connect("tcp://ip:"+porta);
         * ip -> endereço da exchange
         * porta -> porta da exchange
         */

        ComunicaCliente cc = new ComunicaCliente(context, socket, sub);
        (new Thread(cc)).start();
        
        while(!Thread.interrupted()){
            /**
             * Aqui recebe os bytes de subscrição
             */
            byte[] b = socket.recv(0);
            String recebi = new String(b);

            /**
             * Aqui vai receber o resto da mensagem (multiPart)
             */
            if(socket.hasReceiveMore()){

                byte[] n = socket.recv(0);
                try{
                    Notificacao no = Notificacao.parseFrom(n);
                    String msg = "NOTIFICAÇÃO: ";

                    if(no.getTipo() == TipoNotificacao.CRIACAO){
                        msg = msg + "A empresa " + no.getEmpresa() + " criou ";
                        if(no.getTipoMensagem() == TipoAcao.EMISSAO)
                            msg = msg + "uma emissão ";
                        else{
                            if(no.getTipoMensagem() == TipoAcao.LEILAO)
                                msg = msg + "um leilão ";
                            else
                                msg = msg + "erro! ";
                        }
                        msg = msg + "com o montante " + no.getMontante() + ".";
                        if(no.getTaxa() != 0.0)
                            msg = msg + "À taxa de " + no.getTaxa() + ".";
                        if(no.getTempo() != 0)
                            msg = msg + "Com um tempo máximo de " + no.getTempo() + ".";
                        msg = msg + "! O resultado obtido foi: " + no.getResultado().getTexto();
                    }
                    else{
                        if(no.getTipo() == TipoNotificacao.LICITACAO){
                            msg = msg + "Nova licitação ";
                            if(no.getTipoMensagem() == TipoAcao.EMISSAO)
                                msg = msg + "na emissão ";
                            else{
                                if(no.getTipoMensagem() == TipoAcao.LEILAO)
                                    msg = msg + "no leilão ";
                                else
                                    msg = msg + "erro!";
                            }
                            msg = msg + "da empresa " + no.getEmpresa() + "!";
                            msg = msg + no.getResultado().getTexto();
                        }
                        else{
                            if(TipoNotificacao.FIM == no.getTipo()){
                                msg = msg + "Acabou ";
                                if(no.getTipoMensagem() == TipoAcao.EMISSAO)
                                    msg = msg + "a emissão ";
                                else{
                                    if(no.getTipoMensagem() == TipoAcao.LEILAO)
                                        msg = msg + "o leilão ";
                                    else
                                        msg = msg + "erro!";
                                }
                                msg = msg + "da empresa " + no.getEmpresa() + "!";
                                msg = msg + no.getResultado().getTexto();
                            }
                            else
                                msg = msg + "ERRO!!!";
                        }
                    }
                    
                    System.out.println(msg);
                    
                }catch(Exception e){
                    System.out.println("Deu problemas a receber mais!");
                }
                
            }
        }
    }
}