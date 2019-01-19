package exchange;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.FileReader;
import java.util.Iterator;
import org.json.*;



import java.io.*;
import java.util.*;
import java.net.*;

import org.zeromq.ZMQ;

import exchange.CcsEx.*;

public class Exchange{
    //ZMQ.Socket socketExchangePush = context.socket(ZMQ.PUSH);
    //ZMQ.Socket socketNotificacoes = context.socket(ZMQ.PUB);
    //falta o socket de comunicacao com o diretorio

    public static class ExchangeAux{
        String portaPush;
        String portaPull;
        String portaPub;
        String endDir;
        String portaDir;
        ArrayList<String> empresas = new ArrayList<>();
    }

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    public static ExchangeAux parseExchange(String ficheiro){
        try{
            JSONTokener tokener = new JSONTokener(new FileReader(ficheiro));
            JSONObject jo = new JSONObject(tokener);
            ExchangeAux ea = new ExchangeAux();
            
            System.out.println("PortaPush: " + jo.get("portaPush"));
            ea.portaPush = jo.get("portaPush").toString();
            System.out.println("PortaPull: " + jo.get("portaPull"));
            ea.portaPull = jo.get("portaPull").toString();
            System.out.println("PortaPub: " + jo.get("portaPub"));
            ea.portaPub = jo.get("portaPub").toString();
            System.out.println("Diretorio: " + jo.get("enderecoDiretorio"));
            ea.endDir = jo.get("enderecoDiretorio").toString();
            System.out.println("PortaDir: " + jo.get("portaDiretorio"));
            ea.portaDir = jo.get("portaDiretorio").toString();


            JSONArray ja = jo.getJSONArray("empresas");
            System.out.println("-------------EMPRESAS-------------");
            for(int i=0; i<ja.length();i++){
                System.out.println("E: " + ja.get(i));
                ea.empresas.add(ja.get(i).toString());
            }

            return ea;
        }
        catch(Exception e){
            System.out.println(e);
            return null;
        }
        

    }

    public static String getJSON(ExchangeAux ea) throws Exception{
        JSONObject jo = new JSONObject();
        jo.put("endereco",ea.portaPub);
        jo.put("empresas",ea.empresas);

        System.out.println(jo.toString());
        return jo.toString();
    }

    public static void registaExchange(ExchangeAux ea){
        try{
            URL url = new URL("http://" + ea.endDir + 
                            ":" + ea.portaDir + "/exchange");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setInstanceFollowRedirects(false);
            con.setUseCaches(false);

            con.connect();
            OutputStream out = con.getOutputStream();
            String json = getJSON(ea);
            out.write(json.getBytes());
            out.flush();
            out.close();

            int responseCode = con.getResponseCode();
            System.out.println("POST emissao Response Code :: " + responseCode);

        }catch(Exception exc){
            System.out.println(exc);
        }
    }


    public static void main(String[] args){
        if(args.length==0){
            System.out.println("Sem argumentos não há exchange!");
            return;
        }
        System.out.println("====================Chegou ao exchange=====================");
        //carrega de alguma forma as empresas
        //carrega de alguma forma os seus enderecos e portas
        //carrega de alguma forma os enderecos e portas do diretorio e exchange
        ZMQ.Context context = ZMQ.context(1);
        ExchangeAux exca = parseExchange(args[0]);
        if(exca == null){
            System.out.println("Erro no parsing!");
            return;
        }
        
        registaExchange(exca);
        EstruturaExchange estrutura = new EstruturaExchange(context, exca.portaPush,exca.portaPub,
                                                exca.empresas,exca.endDir,exca.portaDir);
        ZMQ.Socket socketExchangePull = context.socket(ZMQ.PULL);
        String myPull = exca.portaPull;
        socketExchangePull.bind("tcp://*:" + myPull);
        socketExchangePull.bind("inproc://terminar");        
        

        while(true){
            byte[] bResposta = socketExchangePull.recv();
            System.out.println("Recebi mensagem");
            System.out.println(bResposta);
            String verifica = new String(bResposta);
            System.out.println(verifica);

            if(verifica.equals("::terminar::")){
                estrutura.termina();
            }
            else{
            try{
                MensagemUtilizador resposta = MensagemUtilizador.parseFrom(bResposta);
                System.out.println("Fiz decode");

                //verifica tipo da mensagem
                if(resposta.getTipoUtilizador() == TipoUtilizador.EMPRESA){
                    if(resposta.getTipo() == TipoMensagem.LEILAO){
                        System.out.println("é leilao de empresa");
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
                exc.printStackTrace();
            }
        }
        }
    }
    
}