package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import cliente.Ccs.*;

public class Empresa{

    String username;
    Socket s;
    BufferedReader inP;
    CodedInputStream cis;
    CodedOutputStream cos;

    public Empresa(String username, Socket s){
        this.username = username;
        this.s = s;
        inP = new BufferedReader(new InputStreamReader(System.in));
        cis = CodedInputStream.newInstance(s.getInputStream());
        cos = CodedOutputStream.newInstance(s.getOutputStream());
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

    public void apresentaCriacaoLeilao(){
        long montante = 0;
        int taxa = 0;
        boolean lido = false;
        do{
            System.out.print("Insira o Montante: ");
            try{
                montante = Long.parseLong(inP.readLine());
                if(montante > 0)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);

        lido = false;

        do{
            System.out.print("Insira a Taxa: ");
            try{
                taxa = Integer.parseInt(inP.readLine());
                if(taxa > 0 && taxa < 101)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);

        float taxaAux = (float) taxa;

        CriacaoLeilao leilao = CriacaoLeilao.newBuilder()
                                            .setMontante(montante)
                                            .setTaxa(taxaAux)
                                            .build();

        MensagemEmpresa mensagem = MensagemEmpresa.newBuilder()
                                                .setTipo(1)
                                                .setLeilao(leilao)
                                                .serUsername(this.username)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Len: " + ba.length);
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        System.out.println("Wrote " + ba.length + " bytes");
        cos.flush();

        System.out.println("-----");
        System.out.println("A esperar resposta por parte do servidor ...");
        System.out.println("-----");

        int len = cis.readRawLittleEndian32();
        len = little2big(len);
        System.out.println("Len: " + len);
        byte[] bResposta = cis.readRawBytes(len);
        System.out.println("Read " + len + " bytes");
        /**
         * A partir daqui tenho de fazer decode da resposta que chegou e apresentar o texto com a mensagem de sucesso ou de erro ...
         */
        //RespostaAutenticacao resposta = RespostaAutenticacao.parseFrom(ba);


    }

    public void apresentaEmissaoTaxaFixa(){
        
        long montante = 0;
        boolean lido = false;
        do{
            System.out.print("Insira o Montante: ");
            try{
                montante = Long.parseLong(inP.readLine());
                if(montante > 0)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);

        EmissaoTaxaFixa emissao = EmissaoTaxaFixa.newBuilder()
                                            .setMontante(montante)
                                            .build();

        MensagemEmpresa mensagem = MensagemEmpresa.newBuilder()
                                                .setTipo(2)
                                                .setEmissao(emissao)
                                                .serUsername(this.username)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Len: " + ba.length);
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        System.out.println("Wrote " + ba.length + " bytes");
        cos.flush();

        System.out.println("-----");
        System.out.println("A esperar resposta por parte do servidor ...");
        System.out.println("-----");

        int len = cis.readRawLittleEndian32();
        len = little2big(len);
        System.out.println("Len: " + len);
        byte[] bResposta = cis.readRawBytes(len);
        System.out.println("Read " + len + " bytes");
        /**
         * A partir daqui tenho de fazer decode da resposta que chegou e apresentar o texto com a mensagem de sucesso ou de erro ...
         */
        //RespostaAutenticacao resposta = RespostaAutenticacao.parseFrom(ba);

    }

    /**
     * Neste menu inicial será apresentada uma interface básica para a empresa
     */
    public void menuInicial(){

        System.out.println("1 - Criar Leilao");
        System.out.println("2 - Emissão Taxa Fixa");
        System.out.print("Opção: ");
        boolean lido = false;
        int opcao = 0;
        while(!lido){
            try{
                opcao = Integer.parseInt(inP.readLine());
                if(opcao == 1 || opcao == 2)
                    lido = true;
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
                System.out.print("Opção: ");
            }
        }

        switch(opcao){
            case 1: apresentaCriacaoLeilao(); break;
            case 2: apresentaEmissaoTaxaFixa(); break;
        }

    }

}

public class ClienteM{

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    /**
     * Devolve null senao estiver autenticado
     * Devolve o papel "empresa" ou "cliente" consoante o papel do utilizador
     */
    public static String leMensagemInicial(CodedInputStream cis){

        try{
            int len = cis.readRawLittleEndian32();
            len = little2big(len);
            System.out.println("Len: " + len);
            byte[] ba = cis.readRawBytes(len);
            System.out.println("Read " + len + " bytes");
            RespostaAutenticacao resposta = RespostaAutenticacao.parseFrom(ba);
            
            boolean sucesso = resposta.getSucesso();
            if( sucesso == true){
                String papel = resposta.getPapel();
                System.out.println("O papel deste utilizador é " + papel);
                return papel;
            }else{
                System.out.println("Utilizador nao valido!");
                return null;
            }
        }
        catch(Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static String autenticaCliente(BufferedReader inP, CodedOutputStream cos){
        try{
            String username, password;
            System.out.println("Username: ");
            username = inP.readLine();

            System.out.println("Password: ");
            password = inP.readLine();

            Autenticacao aut = Autenticacao.newBuilder().
                setUsername(username).
                setPassword(password).
                build();
      
            byte[] ba = aut.toByteArray();

            System.out.println("Len: " + ba.length);
            cos.writeSFixed32NoTag(little2big(ba.length));
            System.out.println("Wrote Len");
            cos.writeRawBytes(ba);
            System.out.println("Wrote " + ba.length + " bytes");
            cos.flush();
            
            return username;

        }catch(Exception e){
            System.out.println(e);
            return null;
        }
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

    public static void main(String args[]) throws Exception{
        Thread outraT = null;
        Scanner sc = new Scanner(System.in);
        try{
            
            /* ------------------------------------------------- */
            
            String host = "localhost";
            Socket s = new Socket(host,12345);

            //BufferedReader in = new BufferedReader(new InputStreamReader( s.getInputStream() ) );
            //PrintWriter out = new PrintWriter( s.getOutputStream() );


            /* ------------------------------ AUTENTICACAO/REGISTO -------------------------------- */

            boolean autenticado = false;
            boolean sair = false;
            BufferedReader inP = new BufferedReader(new InputStreamReader(System.in));

            CodedInputStream cis = CodedInputStream.newInstance(s.getInputStream());
            CodedOutputStream cos = CodedOutputStream.newInstance(s.getOutputStream());

            while(!autenticado){
                System.out.println("1 - Autenticar");
                System.out.println("Outro para sair");

                System.out.print("Opção: ");
                int opcao = 2;

                try{
                    opcao = Integer.parseInt(inP.readLine());
                } catch(Exception e){
                    System.out.println(e);
                }
                
                String user = null;
                String papel = null;

                switch(opcao){
                    //Se calhar só vamos buscar o papel se o user nao for nulo (poed acontecer se der uma exceçao)
                    case 1: user = autenticaCliente(inP,cos); papel = leMensagemInicial(cis); break;
                    default: break;
                }
                
                if(user==null || papel==null){
                    System.out.println("Nao foi validado o login");
                }else{
                    switch(papel){
                        case "empresa": break; //mandar para a empresa
                        case "licitador": break; //mandar para o licitador
                        default: break;
                    }
                }

            }
        }
        finally{
            sc.close();
        }
        
    }
}
