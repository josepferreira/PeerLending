package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.zeromq.ZMQ;

import cliente.Ccs.*;

class RecebeMensagens implements Runnable{

    CodedInputStream cis;

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    public RecebeMensagens(CodedInputStream cis){
        this.cis = cis;
    }

    public void run(){
        try{
            int len = cis.readRawLittleEndian32();
            len = little2big(len);
            byte[] bResposta = cis.readRawBytes(len);
            RespostaExchange resposta = RespostaExchange.parseFrom(bResposta);

            if(resposta.getTipo() == TipoResposta.RESULTADO){
                //Vou imprimir o resultado de um leilao
                Resultado resultado = resposta.getResultado();
                System.out.println("\n -----");
                System.out.println("O resultado do leilão da empresa " + resultado.getEmpresa() + " é: " + resultado.getTexto());
                System.out.println(" -----");
            }else{
                //Vou imprimir a dizer que foi ultrapassado
                NotificacaoUltrapassado notificacao = resposta.getNotificacao();
                System.out.println("\n -----");
                System.out.println("INFO: Foste ultrapassado no leilao da empresa: " + notificacao.getEmpresa());
                System.out.println(" -----");
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

}

class Licitador{


    String username;
    Socket s;
    BufferedReader inP;
    CodedInputStream cis;
    CodedOutputStream cos;
    ZMQ.Context context;

    public Licitador(String username, Socket s) throws Exception{
        this.username = username;
        this.s = s;
        inP = new BufferedReader(new InputStreamReader(System.in));
        cis = CodedInputStream.newInstance(s.getInputStream());
        cos = CodedOutputStream.newInstance(s.getOutputStream());
        (new Thread(new RecebeMensagens(cis))).start();
        GerirSubscricoes gb = new GerirSubscricoes(context);
        Notificacoes n = new Notificacoes(context, gb);
        (new Thread(n)).start();
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

    public void apresentaLicitacaoLeilao() throws Exception{
        String empresa = null;

        do{
            System.out.println("Insira a empresa: ");
            empresa = inP.readLine();
        }while(empresa != null);
        
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

        LicitacaoLeilao leilao = LicitacaoLeilao.newBuilder()
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxaAux)
                                            .build();

        MensagemInvestidor mensagem = MensagemInvestidor.newBuilder()
                                                .setTipo(TipoMensagem.LEILAO)
                                                .setLeilao(leilao)
                                                .setUtilizador(this.username)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Len: " + ba.length);
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        System.out.println("Wrote " + ba.length + " bytes");
        cos.flush();

        // System.out.println("-----");
        // System.out.println("A esperar resposta por parte do servidor ...");
        // System.out.println("-----");

        // // int len = cis.readRawLittleEndian32();
        // len = little2big(len);
        // System.out.println("Len: " + len);
        // byte[] bResposta = cis.readRawBytes(len);
        // System.out.println("Read " + len + " bytes");
        // /**
        //  * A partir daqui tenho de fazer decode da resposta que chegou e apresentar o texto com a mensagem de sucesso ou de erro ...
        //  */
        // Resultado resposta = Resultado.parseFrom(bResposta);
        // System.out.println("O resultado da licitacao do leilao foi " + resposta.getTexto());


    }

    public void apresentaSubscricaoTaxaFixa() throws Exception{
        String empresa = null;

        do{
            System.out.println("Insira a empresa: ");
            empresa = inP.readLine();
        }while(empresa != null);

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

        SubscricaoTaxaFixa emissao = SubscricaoTaxaFixa.newBuilder()
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .build();

        MensagemInvestidor mensagem = MensagemInvestidor.newBuilder()
                                                .setTipo(TipoMensagem.EMISSAO)
                                                .setEmissao(emissao)
                                                .setUtilizador(this.username)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Len: " + ba.length);
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        System.out.println("Wrote " + ba.length + " bytes");
        cos.flush();

        // System.out.println("-----");
        // System.out.println("A esperar resposta por parte do servidor ...");
        // System.out.println("-----");

        // int len = cis.readRawLittleEndian32();
        // len = little2big(len);
        // System.out.println("Len: " + len);
        // byte[] bResposta = cis.readRawBytes(len);
        // System.out.println("Read " + len + " bytes");
        // /**
        //  * A partir daqui tenho de fazer decode da resposta que chegou e apresentar o texto com a mensagem de sucesso ou de erro ...
        //  */
        // //RespostaAutenticacao resposta = RespostaAutenticacao.parseFrom(ba);

    }


    /**
     * Neste menu inicial será apresentada uma interface básica para a empresa
     */
    public void menuInicial() throws Exception {
        boolean continua = true;
        while(continua){

            System.out.println("1 - Criar Leilao");
            System.out.println("2 - Emissão Taxa Fixa");
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
                case 1: apresentaLicitacaoLeilao(); break;
                case 2: apresentaSubscricaoTaxaFixa(); break;
                default: continua = false;
            }
        }

    }

}

 class Empresa{


    String username;
    Socket s;
    BufferedReader inP;
    CodedInputStream cis;
    CodedOutputStream cos;
    GerirSubscricoes subscricoes;
    ZMQ.Context context = ZMQ.context(1);

    public Empresa(String username, Socket s) throws Exception{
        this.username = username;
        this.s = s;
        inP = new BufferedReader(new InputStreamReader(System.in));
        cis = CodedInputStream.newInstance(s.getInputStream());
        cos = CodedOutputStream.newInstance(s.getOutputStream());
        (new Thread(new RecebeMensagens(cis))).start();
        GerirSubscricoes gb = new GerirSubscricoes(context);
        Notificacoes n = new Notificacoes(context, gb);
        (new Thread(n)).start();

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

    public void apresentaCriacaoLeilao() throws Exception{
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
                                                .setTipo(TipoMensagem.LEILAO)
                                                .setLeilao(leilao)
                                                .setUtilizador(this.username)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Len: " + ba.length);
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        System.out.println("Wrote " + ba.length + " bytes");
        cos.flush();

        // System.out.println("-----");
        // System.out.println("A esperar resposta por parte do servidor ...");
        // System.out.println("-----");

        // // int len = cis.readRawLittleEndian32();
        // // len = little2big(len);
        // // System.out.println("Len: " + len);
        // // byte[] bResposta = cis.readRawBytes(len);
        // // System.out.println("Read " + len + " bytes");
        // // /**
        // //  * A partir daqui tenho de fazer decode da resposta que chegou e apresentar o texto com a mensagem de sucesso ou de erro ...
        // //  */
        // // Resultado resposta = Resultado.parseFrom(bResposta);
        // // System.out.println("O resultado da criacao do leilao foi " + resposta.getTexto());


    }

    public void apresentaEmissaoTaxaFixa() throws Exception{
        
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
                                                .setTipo(TipoMensagem.EMISSAO)
                                                .setEmissao(emissao)
                                                .setUtilizador(this.username)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        System.out.println("Len: " + ba.length);
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        System.out.println("Wrote " + ba.length + " bytes");
        cos.flush();

        // System.out.println("-----");
        // System.out.println("A esperar resposta por parte do servidor ...");
        // System.out.println("-----");

        // int len = cis.readRawLittleEndian32();
        // len = little2big(len);
        // System.out.println("Len: " + len);
        // byte[] bResposta = cis.readRawBytes(len);
        // System.out.println("Read " + len + " bytes");
        // /**
        //  * A partir daqui tenho de fazer decode da resposta que chegou e apresentar o texto com a mensagem de sucesso ou de erro ...
        //  */
        // //RespostaAutenticacao resposta = RespostaAutenticacao.parseFrom(ba);

    }


    /**
     * Neste menu inicial será apresentada uma interface básica para a empresa
     */
    public void menuInicial() throws Exception {
        boolean continua = true;
        while(continua){

            System.out.println("1 - Criar Leilao");
            System.out.println("2 - Emissão Taxa Fixa");
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
                case 1: apresentaCriacaoLeilao(); break;
                case 2: apresentaEmissaoTaxaFixa(); break;
                default: continua = false;
            }
        }

    }

}

 class ClienteM{

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
                System.out.println("2 - Visualizar Leilões Ativos");
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
                    case 2: System.out.println("A funcionalidade ainda nao esta implementada"); break;
                    default: autenticado = true; break;
                }
                
                if(user==null || papel==null){
                    System.out.println("bye!");
                }else{
                    switch(papel){
                        case "empresa": autenticado=true; System.out.println("É uma empresa"); (new Empresa(user, s)).menuInicial(); System.out.println("bye!"); break; //mandar para a empresa
                        case "licitador": autenticado=true; System.out.println("É um licitador"); (new Licitador(user, s)).menuInicial(); System.out.println("bye!"); break; //mandar para o licitador
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
