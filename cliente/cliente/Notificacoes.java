package cliente;

import java.io.*;
import java.util.*;
import java.net.*;
import java.time.LocalDateTime;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.zeromq.ZMQ;
import org.json.*;

import cliente.CcsCliente.*;
import cliente.NotificacaoCliente.*;
import java.awt.Desktop;

/**
 * Vou ter de ter duas threads:
 * 1) Para comunicar com o Cliente (inproc)
 *      Tratará de adicionar uma subscrição ... 
 * 2) Para comunicar com o Exchange
 */

class ComunicaCliente{

    private ZMQ.Context context;
    private ZMQ.Socket sub;
    private GerirSubscricoes subscricao;
    private HashMap<String, ArrayList<String>> enderecos = new HashMap<>();
    private boolean temTodos = false;
    Enderecos end;

    public ComunicaCliente(ZMQ.Context context, ZMQ.Socket sub, GerirSubscricoes subscricao, Enderecos end){
        this.context = context;
        this.sub = sub;
        this.subscricao = subscricao;
        this.end = end;
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
        try{
            URL url = new URL("http://" + end.enderecoDiretorio + 
                                        ":" + end.portaDiretorio + "/exchange/todas");
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
            for(int ij=0; ij<jsonArray.length(); ij++){
                JSONObject exchange = jsonArray.getJSONObject(ij);
                String endExchange = exchange.get("endereco").toString();
                JSONArray ja = exchange.getJSONArray("empresas");
                ArrayList<String> empresas = new ArrayList<>();

                for(int i=0; i<ja.length();i++){
                    System.out.println("E: " + ja.get(i));
                    empresas.add(ja.get(i).toString());
                }

                enderecos.put(endExchange, empresas);
                System.out.println(endExchange);
                this.sub.connect("tcp://*:"+endExchange); //e preciso ver isto do asteristo
                // MUITO PRECISO MESMO
                // QUASE TANTE COMO O JJ BOCE
                //88 - oitchenta e otcho
            }
            temTodos = true;

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    private void registaNovoEndereco(String empresa){
        System.out.println("Vou subscrever a empresa " + empresa);
        
        try{
            URL url = new URL("http://" + end.enderecoDiretorio + 
                                    ":" + end.portaDiretorio + "/exchange?empresa="+empresa);
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

            System.out.println(response.toString());
            JSONObject exchange = new JSONObject(response.toString());
            String endExchange = exchange.get("endereco").toString();
            JSONArray ja = exchange.getJSONArray("empresas");
            ArrayList<String> empresas = new ArrayList<>();

            for(int i=0; i<ja.length();i++){
                System.out.println("E: " + ja.get(i));
                empresas.add(ja.get(i).toString());
            }

            enderecos.put(endExchange, empresas);
            this.sub.connect("tcp://*:"+endExchange); //e preciso ver isto do asteristo
            // MUITO PRECISO MESMO
            // QUASE TANTE COMO O JJ BOCE
            //88 - oitchenta e otcho

        }catch(Exception exc){
            System.out.println(exc);
        }
    

    }

    public void atoSub(String msgResposta){


            /**
             * Tenho de fazer parse à mensagem
             * Se for sub@ -> ENtao quer dizer que é para subscrever
             * Se for unsub@ -> então é para tirar o subscribe
             */

            System.out.println("RECEBI A SEGUINTE MENSAGEM: " + msgResposta);
            String decisao = msgResposta.split("@")[0];
            String subResposta = msgResposta.split("@")[1];
            System.out.println("\nA decisao é " + decisao + ". A subscrição: " + subResposta);
            /**
             * Vou adicionar a subscricao à classe subscricao e ao socket
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

public class Notificacoes implements Runnable{

    GerirSubscricoes sub;
    ZMQ.Context context;
    Enderecos enderecos;
    ComunicaCliente cc;
    String username;

    public Notificacoes(ZMQ.Context context, GerirSubscricoes gb, Enderecos end, String username){
        this.context = context;
        this.sub = gb;
        enderecos = end;
        this.username = username;
    }

    public void run(){
        //Vou ter de me associar às exchanges
        /*try{
            FileWriter fw = new FileWriter("notificacoes-" + this.username + ".txt", true); //true para fazer append
            fw.write("\n:::::::::::::::::::::::::::::::Novo início de sessão!:::::::::::::::::::::::\n\n");
            fw.close();
            File file = new File("notificacoes-" + this.username + ".txt");
            Desktop.getDesktop().open(file);
        }
        catch(Exception exce){
            System.out.println(exce);
        }
        */
        ZMQ.Socket socket = context.socket(ZMQ.SUB);
        socket.connect("inproc://notificacoes");
        socket.subscribe("comuSub");

        cc = new ComunicaCliente(context, socket, sub, enderecos);
        
        while(!Thread.interrupted()){
            /**
             * Aqui recebe os bytes de subscrição
             */
            
            byte[] b = socket.recv(0);
            String recebi = new String(b);
            System.out.println("Recebi: " + recebi);

            if(recebi.startsWith("comuSub")){
                cc.atoSub(recebi.substring(7));
            }

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
                        if(no.hasTaxa())
                            msg = msg + "À taxa de " + no.getTaxa() + ".";
                        if(no.hasTempo())
                            msg = msg + "Com um tempo máximo de " + no.getTempo() + ".";
                        ResultadoAcao aux = no.getResultado();
                        if(aux.hasTexto())
                            msg = msg + "! O resultado obtido foi: " + aux.getTexto();
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
                            ResultadoAcao aux = no.getResultado();
                            if(aux.hasTexto())
                                msg = msg + aux.getTexto();
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
                                ResultadoAcao aux = no.getResultado();
                                msg = " O resultado obtido foi " + (aux.getSucesso() ? " sucesso! " : " insucesso!");
                                if(aux.hasTexto())
                                    msg = msg + aux.getTexto();
                            }
                            else
                                msg = msg + "ERRO!!!";
                        }
                    }
                    
                    //System.out.println(msg);
                    try {
                        FileWriter fw = new FileWriter("notificacoes-" + this.username + ".txt", true); //true para fazer append
                        fw.write((LocalDateTime.now()) + " :: " + msg + "\n");
                        fw.close();
                    } catch (Exception e) {
                        System.out.println("Erro ao escrever as notificações no ficheiro! " + e.getMessage());
                    }
                    
                    
                }catch(Exception e){
                    System.out.println("Deu problemas a receber mais!");
                }
                
            }
        }
    }
}