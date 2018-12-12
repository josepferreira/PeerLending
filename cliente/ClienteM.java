package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import cliente.Ccs.*;


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
