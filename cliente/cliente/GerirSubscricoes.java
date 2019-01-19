package cliente;

import java.io.*;
import java.util.*;
import java.net.*;
import org.json.*;

import org.zeromq.ZMQ;
import com.google.protobuf.CodedOutputStream;

import cliente.CcsCliente.*;

/**
 * Ira apresentar o menu das subscricoes e será responsável em comunicar com a classe Notificacaoes
 */
public class GerirSubscricoes{
    
    boolean leiloesSubscritos;
    boolean emissoesSubscritas;
    HashSet<String> empresasSubscritas;
    BufferedReader inP = new BufferedReader(new InputStreamReader(System.in));
    ZMQ.Socket socket;
    String headSub = "comuSub";
    CodedOutputStream cos;
    String username;
    String papel;


    public GerirSubscricoes(ZMQ.Context context, boolean leilao, boolean emissao, List<String> emps, CodedOutputStream cos, String username, String papel){
        this.socket = context.socket(ZMQ.PUB);
        this.socket.bind("inproc://notificacoes");
        this.leiloesSubscritos = leilao;
        this.emissoesSubscritas = emissao;
        this.empresasSubscritas = new HashSet<>();
        if(emps != null)
        for(String s: emps)
            this.empresasSubscritas.add(s);
        this.cos = cos;
        this.username = username;
        this.papel = papel;

        if(this.leiloesSubscritos){
            socket.send(headSub + "sub@emissao::");   
        }
        if(this.emissoesSubscritas){
            socket.send(headSub + "sub@emissao::");
        }
        for(String emp: empresasSubscritas){
            socket.send(headSub + "sub@leilao::" + emp + "::");
            socket.send(headSub + "sub@emissao::" + emp + "::");
        }
    }

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    public static int lerOpcao(Scanner sc){
        boolean lido = false;
        int opcao = -1;
        
        while(!lido){

            try{
                opcao = sc.nextInt();
                lido = true;
                System.out.println("OPCAO: " + opcao);
            }
            catch(Exception e){}
        }
        //sc.close();

        return opcao;
    }

    public void adicionaEmpresa(String empresa){
        empresasSubscritas.add(empresa);
        
        System.out.println("\n\nVou enviar mensagem para sibscrever 1 empresa\n\n");
        
        Subscricao leilao = Subscricao.newBuilder()
                                            .setTipo(TipoSubscricao.EMPRESASUB)
                                            .setESubscricao(true)
                                            .setEmpresa(empresa)
                                            .build();

        MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                            .setTipo(TipoMensagem.SUBSCRICAO)
                                            .setTipoUtilizador(this.papel.equals("empresa") ? TipoUtilizador.EMPRESA : TipoUtilizador.INVESTIDOR)
                                            .setUtilizador(this.username)
                                            .setSubscricao(leilao)
                                            .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Mensagem: " + ba);
        System.out.println("Tamanho: " + ba.length);
        System.out.println("Tamanho: " + little2big(ba.length));

        try{
            cos.writeSFixed32NoTag(little2big(ba.length));
            cos.writeRawBytes(ba);
            cos.flush();
        }
        catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void removeEmpresa(String empresa){
        empresasSubscritas.remove(empresa);

        Subscricao leilao = Subscricao.newBuilder()
                                            .setTipo(TipoSubscricao.EMPRESASUB)
                                            .setESubscricao(false)
                                            .setEmpresa(empresa)
                                            .build();

        MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                            .setTipo(TipoMensagem.SUBSCRICAO)
                                            .setTipoUtilizador(this.papel.equals("empresa") ? TipoUtilizador.EMPRESA : TipoUtilizador.INVESTIDOR)
                                            .setUtilizador(this.username)
                                            .setSubscricao(leilao)
                                            .build();
      
        byte[] ba = mensagem.toByteArray();

        try{
            cos.writeSFixed32NoTag(little2big(ba.length));
            cos.writeRawBytes(ba);
            cos.flush();
        }
        catch(Exception exc){
            System.out.println(exc);
        }
    }

    private void apresentaListaSubscricao(){

        if(leiloesSubscritos)
            System.out.println("Tem todos os leilões subscritos!");
        if(emissoesSubscritas)
            System.out.println("Tem todos as emissões subscritas!");
        System.out.println("Empresas subscritas: ");
        int i = 1;
        for(String s: empresasSubscritas){
            System.out.println(i++ + ": " + s);
        }

    }

    private void acrescentaSubscricao(){

        apresentaListaSubscricao();
        boolean continua = true;
        int opcao = 0;

        do{
            System.out.println("1 - Subscreve todos os leiloes");
            System.out.println("2 - Subscreve todas as emissões");
            System.out.println("3 - Subscreve uma empresa");
            System.out.println("0 - Voltar");
            System.out.print("Opção: ");
            
            try{
                opcao = Integer.parseInt(inP.readLine());
                
                switch(opcao){
                    case 1:
                        if(leiloesSubscritos == false){
                            System.out.println("Vou subscrever todos os leilões");
                            socket.send(headSub + "sub@leilao::");
                            leiloesSubscritos = true;
                            
                            Subscricao leilao = Subscricao.newBuilder()
                                            .setTipo(TipoSubscricao.LEILAOSUB)
                                            .setESubscricao(true)
                                            .build();

                            MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                                                .setTipo(TipoMensagem.SUBSCRICAO)
                                                                .setTipoUtilizador(this.papel.equals("empresa") ? TipoUtilizador.EMPRESA : TipoUtilizador.INVESTIDOR)
                                                                .setUtilizador(this.username)
                                                                .setSubscricao(leilao)
                                                                .build();
                        
                            byte[] ba = mensagem.toByteArray();

                            try{
                                cos.writeSFixed32NoTag(little2big(ba.length));
                                cos.writeRawBytes(ba);
                                cos.flush();
                            }
                            catch(Exception exc){
                                System.out.println(exc);
                            }


                        }else{
                            System.out.println("ERRO: Leilões já se encontram subscritos ... Ação inválida!");
                        }
                        break;
                    case 2:
                        if(emissoesSubscritas == false){
                            System.out.println("Vou subscrever todos as emissões");
                            socket.send(headSub + "sub@emissao::");
                            emissoesSubscritas = true;

                            Subscricao leilao = Subscricao.newBuilder()
                                            .setTipo(TipoSubscricao.EMISSAOSUB)
                                            .setESubscricao(true)
                                            .build();

                            MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                                                .setTipo(TipoMensagem.SUBSCRICAO)
                                                                .setTipoUtilizador(this.papel.equals("empresa") ? TipoUtilizador.EMPRESA : TipoUtilizador.INVESTIDOR)
                                                                .setUtilizador(this.username)
                                                                .setSubscricao(leilao)
                                                                .build();
                        
                            byte[] ba = mensagem.toByteArray();

                            try{
                                cos.writeSFixed32NoTag(little2big(ba.length));
                                cos.writeRawBytes(ba);
                                cos.flush();
                            }
                            catch(Exception exc){
                                System.out.println(exc);
                            }
                        }else{
                            System.out.println("ERRO: Emissões já se encontram subscritas ... Ação inválida!");
                        }
                        break;
                    case 3:
                        if(empresasSubscritas.size() > 5){
                            System.out.println("Já atingiu o limite de 5 empresas subscritas!");
                            break;   
                        }
                        System.out.print("Escreva o nome da empresa: ");
                        String empresa = inP.readLine();
                        if(!empresasSubscritas.contains(empresa)){
                            System.out.println("Vou mandar uma empresa para subscrever! " + empresa);
                            socket.send(headSub + "sub@leilao::" + empresa + "::");
                            socket.send(headSub + "sub@emissao::" + empresa + "::");
                            adicionaEmpresa(empresa);
                        }else{
                            System.out.println("ERRO: Empresa já se encontra subscrita ... Ação inválida!");
                        }
                        break;
                    case 0: continua = false; break;
                    default:
                        System.out.println("Opção Inválida!");
                }


            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
                System.out.print("Opção: ");
            }
            
        }while(continua);
    }

    private void retiraSubscricao(){

        apresentaListaSubscricao();
        boolean continua = true;
        int opcao = 0;

        do{
            System.out.println("1 - Vetar todos os leiloes");
            System.out.println("2 - Vetar todas as emissões");
            System.out.println("3 - Vetar uma empresa");
            System.out.print("Opção: ");
            
            try{
                opcao = Integer.parseInt(inP.readLine());
                
                switch(opcao){
                    case 1:
                        if(leiloesSubscritos == true){
                            System.out.println("Vou vetar todos os leilões");
                            socket.send(headSub + "unsub@leilao::");
                            leiloesSubscritos = false;

                            Subscricao leilao = Subscricao.newBuilder()
                                            .setTipo(TipoSubscricao.LEILAOSUB)
                                            .setESubscricao(false)
                                            .build();

                            MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                                                .setTipo(TipoMensagem.SUBSCRICAO)
                                                                .setTipoUtilizador(this.papel.equals("empresa") ? TipoUtilizador.EMPRESA : TipoUtilizador.INVESTIDOR)
                                                                .setUtilizador(this.username)
                                                                .setSubscricao(leilao)
                                                                .build();
                        
                            byte[] ba = mensagem.toByteArray();

                            try{
                                cos.writeSFixed32NoTag(little2big(ba.length));
                                cos.writeRawBytes(ba);
                                cos.flush();
                            }
                            catch(Exception exc){
                                System.out.println(exc);
                            }

                        }else{
                            System.out.println("ERRO: Leilões não se encontram subscritos ... Ação inválida!");
                        }
                        continua = false;
                        break;
                    case 2:
                        if(emissoesSubscritas == true){
                            System.out.println("Vou vetar todos as emissões");
                            socket.send(headSub + "unsub@emissao::");
                            emissoesSubscritas = false;

                            Subscricao leilao = Subscricao.newBuilder()
                                            .setTipo(TipoSubscricao.EMISSAOSUB)
                                            .setESubscricao(false)
                                            .build();

                            MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                                                .setTipo(TipoMensagem.SUBSCRICAO)
                                                                .setTipoUtilizador(this.papel.equals("empresa") ? TipoUtilizador.EMPRESA : TipoUtilizador.INVESTIDOR)
                                                                .setUtilizador(this.username)
                                                                .setSubscricao(leilao)
                                                                .build();
                        
                            byte[] ba = mensagem.toByteArray();

                            try{
                                cos.writeSFixed32NoTag(little2big(ba.length));
                                cos.writeRawBytes(ba);
                                cos.flush();
                            }
                            catch(Exception exc){
                                System.out.println(exc);
                            }

                        }else{
                            System.out.println("ERRO: Emissões não se encontram subscritas ... Ação inválida!");
                        }
                        continua = false;
                        break;
                    case 3:
                        System.out.print("Escreva o nome da empresa: ");
                        String empresa = inP.readLine();
                        if(empresasSubscritas.contains(empresa)){
                            System.out.println("Vou mandar uma empresa para vetar! " + empresa);
                            socket.send(headSub + "unsub@leilao::" + empresa + "::");
                            socket.send(headSub + "unsub@emissao::" + empresa + "::");
                            removeEmpresa(empresa);
                        }else{
                            System.out.println("ERRO: Empresa não se encontra subscrita ... Ação inválida!");
                        }
                        continua = false;
                        break;
                    default:
                        System.out.println("Opção Inválida!");
                }


            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
                System.out.print("Opção: ");
            }
            
        }while(continua);
    }



    public void menuInicial() throws Exception {
        boolean continua = true;
        
        while(continua){

            System.out.println("1 - Lista de Subscrições");
            System.out.println("2 - Adicionar uma Subscrição");
            System.out.println("3 - Retirar uma Subscrição");
            System.out.print("Opção: ");
            boolean lido = false;
            int opcao = 0;
            while(!lido){
                try{
                    opcao = Integer.parseInt(inP.readLine());
                    lido = true;
                } catch(Exception e){
                    System.out.println("O valor introduzido não é valido!");
                    System.out.print("Opção: ");
                }
            }

            switch(opcao){
                case 1: apresentaListaSubscricao(); break;
                case 2: acrescentaSubscricao(); break;
                case 3: retiraSubscricao(); break;
                default: continua = false;
            }
        }

    }



}