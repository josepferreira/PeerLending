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

    public static void autenticaCliente(BufferedReader inP, CodedOutputStream cos){
        try{
            String username, password;
            System.out.println("Username: ");
            username = inP.readLine();

            System.out.println("Password: ");
            password = inP.readLine();

            //construir mensagem;

            //System.out.println("Email: ");
            
            //System.out.println("Nome: ");

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
        }catch(Exception e){
            System.out.println(e);
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

                switch(opcao){
                    case 1: autenticaCliente(inP,cos);// autenticado = leMensagemInicial(cis); break;
                    default: break;//deu merda e é para sair
                }

            }
        }
        finally{
            sc.close();
        }
        
    }
}
