package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.zeromq.ZMQ;

import org.json.*;



import cliente.CcsCliente.*;

class Informacoes{
    Enderecos end;
    BufferedReader inP;

    public Informacoes(Enderecos end){
        this.end = end;
        this.inP = new BufferedReader(new InputStreamReader(System.in));
    }

    private void selecionaDeTodos(String acao, boolean ativo){
        switch(acao){
            case "leilao":
                if(ativo)
                    end.leiloesAtivos();
                else
                    end.leiloesFinalizados();
                break;
            case "emissao":
                if(ativo)
                    end.emissoesAtivas();
                else
                    end.emissoesFinalizadas();
        }
    }

    private void selecionaDeEmpresa(String acao, boolean ativo){
        String empresa = null;
        boolean lido = false;
        do{
            System.out.print("Insira a Empresa: ");
            try{
                empresa = inP.readLine();
                if(empresa.length() > 0)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);
        
        switch(acao){
            case "leilao":
                if(ativo)
                    end.leiloesAtivos(empresa);
                else
                    end.leiloesFinalizados(empresa);
                break;
            case "emissao":
                if(ativo)
                    end.emissoesAtivas(empresa);
                else
                    end.emissoesFinalizadas(empresa);
        }
    }

    private void apresentaOpcoes(String acao, boolean ativo){
        boolean continua = true;
        while(continua){

            System.out.println("1 - Visualizar todos!");
            System.out.println("2 - Visualizar de uma Empresa!");
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
                case 1: selecionaDeTodos(acao, ativo); break;
                case 2: selecionaDeEmpresa(acao, ativo); break;
                default: continua = false;
            }
        }
        
    }

    public void menuInicial() {
        boolean continua = true;
        while(continua){

            System.out.println("1 - Visualizar Leilões Ativos");
            System.out.println("2 - Visualizar Emissões Ativas");
            System.out.println("3 - Visualizar Leilões Terminados");
            System.out.println("4 - Visualizar Emissões Terminadas");
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
                case 1: apresentaOpcoes("leilao", true); break;
                case 2: apresentaOpcoes("emissao", true); break;
                case 3: apresentaOpcoes("leilao", false); break;
                case 4: apresentaOpcoes("emissao", false); break;
                default: continua = false;
            }
        }

    }
}

class RecebeMensagens implements Runnable{

    CodedInputStream cis;

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    public RecebeMensagens(CodedInputStream cis){
        this.cis = cis;
    }

    public void run(){
        while(!Thread.interrupted()){
            try{
                int len = cis.readRawLittleEndian32();
                len = little2big(len);
                byte[] bResposta = cis.readRawBytes(len);
                RespostaExchange resposta = RespostaExchange.parseFrom(bResposta);

                if(resposta.getTipo() == TipoResposta.RESULTADO){
                    //Vou imprimir o resultado de um leilao
                    Resultado resultado = resposta.getResultado();
                    System.out.println("\n -----");
                    String resultadoMsg = resultado.hasTexto() ? resultado.getTexto() : "Impossivel apresentar";
                    System.out.println("O resultado do leilão da empresa " + resultado.getEmpresa() + " é: " + (resultadoMsg.equals("[]") ? "sem qualquer proposta apresentada!" : resultadoMsg));
                    System.out.println(" O resultado obtido foi " + (resultado.getSucesso() ? " sucesso! " : " insucesso!"));
                    System.out.println(" -----");
                }else{
                    if(resposta.getTipo() == TipoResposta.RESPOSTA){
                        Resposta r = resposta.getResposta();
                        String pal = null;
                        if(r.getTipo() == TipoMensagem.LEILAO)
                            pal = "licitação";
                        else
                            pal = "subscrição";
                        System.out.println("O resultado da sua " + pal + " é: " + (r.getSucesso() ? "sucesso!" : "insucesso"));
                        String msg = r.hasMensagem() ? r.getMensagem() : null;
                        if (msg != null){
                            System.out.println("Mensagem adicional:  " + msg);
                        }
                        
                    }else{
                        //Vou imprimir a dizer que foi ultrapassado
                        NotificacaoUltrapassado notificacao = resposta.getNotificacao();
                        System.out.println("\n -----");
                        System.out.println("INFO: Foi ultrapassado no leilao da empresa: " + notificacao.getEmpresa());
                        System.out.println("O valor é de " + notificacao.getValor());
                        if(notificacao.hasTexto())
                            System.out.println("Info adicional: " + notificacao.getTexto());
                        System.out.println(" -----");
                    }
                }
            }
            catch(Exception e){
                System.out.println("Houve uma exceção: " + e);
            }
        }
    }

}

class Licitador{


    String username;
    Socket s;
    BufferedReader inP;
    CodedInputStream cis;
    CodedOutputStream cos;
    GerirSubscricoes subscricoes;
    ZMQ.Context context = ZMQ.context(1);
    //Enderecos enderecos;
    Informacoes informacoes;

    public Licitador(String username, Socket s, Enderecos enderecos, boolean leilao, boolean emissao, List<String> emps) throws Exception{
        this.username = username;
        this.s = s;
        inP = new BufferedReader(new InputStreamReader(System.in));
        cis = CodedInputStream.newInstance(s.getInputStream());
        cos = CodedOutputStream.newInstance(s.getOutputStream());
        (new Thread(new RecebeMensagens(cis))).start();
        subscricoes = new GerirSubscricoes(context, leilao, emissao, emps, cos, username, "licitador");
        Notificacoes n = new Notificacoes(context, subscricoes,enderecos,username);
        (new Thread(n)).start();
        subscricoes.ativaSubscricoes();
        informacoes = new Informacoes(enderecos);
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
        }
        while(empresa == null);
        
        long montante = 0;
        float taxa = 0;
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
                taxa = Float.parseFloat(inP.readLine());
                if(taxa > 0 && taxa < 101)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);

        LicitacaoLeilao leilao = LicitacaoLeilao.newBuilder()
                                            .setEmpresa(empresa)
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .build();

        MensagemInvestidor mensagemInvestidor = MensagemInvestidor.newBuilder()
                                                .setLeilao(leilao)
                                                .build();

        MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                                .setTipo(TipoMensagem.LEILAO)
                                                .setTipoUtilizador(TipoUtilizador.INVESTIDOR)
                                                .setUtilizador(this.username)
                                                .setInvestidor(mensagemInvestidor)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        
        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        cos.flush();

    }

    public void apresentaSubscricaoTaxaFixa() throws Exception{
        String empresa = null;
        System.out.println("Subscricao!");

        do{
            System.out.println("Insira a empresa: ");
            empresa = inP.readLine();
        }while(empresa == null);

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

        MensagemInvestidor mensagemInvestidor = MensagemInvestidor.newBuilder()
                                                .setEmissao(emissao)
                                                .build();

        MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                                .setTipo(TipoMensagem.EMISSAO)
                                                .setTipoUtilizador(TipoUtilizador.INVESTIDOR)
                                                .setUtilizador(this.username)
                                                .setInvestidor(mensagemInvestidor)
                                                .build();
      
        byte[] ba = mensagem.toByteArray();

        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        cos.flush();

    }


    /**
     * Neste menu inicial será apresentada uma interface básica para a empresa
     */
    public void menuInicial() throws Exception {
        boolean continua = true;
        while(continua){

            System.out.println("1 - Licitar Leilao");
            System.out.println("2 - Emissão Taxa Fixa");
            System.out.println("3 - Gerir Subscrições");
            System.out.println("4 - Informações");
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
                case 3: subscricoes.menuInicial(); break;
                case 4: informacoes.menuInicial(); break;
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
    Informacoes informacoes;

    public Empresa(String username, Socket s, Enderecos enderecos, boolean leilao, boolean emissao, List<String> emps) throws Exception{
        this.username = username;
        this.s = s;
        inP = new BufferedReader(new InputStreamReader(System.in));
        cis = CodedInputStream.newInstance(s.getInputStream());
        cos = CodedOutputStream.newInstance(s.getOutputStream());
        (new Thread(new RecebeMensagens(cis))).start();
        subscricoes = new GerirSubscricoes(context, leilao, emissao, emps, cos, username, "empresa");
        Notificacoes n = new Notificacoes(context, subscricoes,enderecos, username);
        (new Thread(n)).start();
        subscricoes.ativaSubscricoes();
        informacoes = new Informacoes(enderecos);

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
        float taxa = 0;
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
                taxa = Float.parseFloat(inP.readLine());
                if(taxa > 0 && taxa < 101)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);

        lido = false;
        long tempo = 0;
        do{
            System.out.print("Insira o Tempo: ");
            try{
                tempo = Long.parseLong(inP.readLine());
                if(tempo > 0)
                    lido = true;
                /**
                 * Tenho de validar se é multiplo de 100?
                 */
            } catch(Exception e){
                System.out.println("O valor introduzido não é valido!");
            }
        }while(!lido);

        CriacaoLeilao leilao = CriacaoLeilao.newBuilder()
                                            .setMontante(montante)
                                            .setTaxa(taxa)
                                            .setTempo(tempo)
                                            .build();

        MensagemEmpresa mensagemEmpresa = MensagemEmpresa.newBuilder()
                                                .setLeilao(leilao)
                                                .build();

        MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                            .setTipo(TipoMensagem.LEILAO)
                                            .setTipoUtilizador(TipoUtilizador.EMPRESA)
                                            .setUtilizador(this.username)
                                            .setEmpresa(mensagemEmpresa)
                                            .build();
      
        byte[] ba = mensagem.toByteArray();

        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        cos.flush();
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

        lido = false;
        long tempo = 0;
        do{
            System.out.print("Insira o Tempo: ");
            try{
                tempo = Long.parseLong(inP.readLine());
                if(tempo > 0)
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
                                            .setTempo(tempo)
                                            .build();

        MensagemEmpresa mensagemEmpresa = MensagemEmpresa.newBuilder()
                                                .setEmissao(emissao)
                                                .build();
        
        MensagemUtilizador mensagem = MensagemUtilizador.newBuilder()
                                            .setTipo(TipoMensagem.EMISSAO)
                                            .setTipoUtilizador(TipoUtilizador.EMPRESA)
                                            .setUtilizador(this.username)
                                            .setEmpresa(mensagemEmpresa)
                                            .build();
      
        byte[] ba = mensagem.toByteArray();

        cos.writeSFixed32NoTag(little2big(ba.length));
        cos.writeRawBytes(ba);
        cos.flush();

    }


    /**
     * Neste menu inicial será apresentada uma interface básica para a empresa
     */
    public void menuInicial() throws Exception {
        boolean continua = true;
        while(continua){

            System.out.println("1 - Criar Leilao");
            System.out.println("2 - Emissão Taxa Fixa");
            System.out.println("3 - Gerir Subscrições");
            System.out.println("4 - Informações");
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
                case 3: subscricoes.menuInicial(); break;
                case 4: informacoes.menuInicial(); break;
                default: continua = false;
            }
        }

    }

}

 class ClienteM{

    public static Enderecos enderecos;

    public static Enderecos parseEnderecos(String ficheiro){
        try{
            JSONTokener tokener = new JSONTokener(new FileReader(ficheiro));
            JSONObject jo = new JSONObject(tokener);
            Enderecos ea = new Enderecos();
            
            ea.enderecoDiretorio = jo.get("enderecoDiretorio").toString();
            ea.portaDiretorio = jo.get("portaDiretorio").toString();
            ea.portaFrontEnd = jo.get("portaFrontEnd").toString();
            ea.enderecoFrontEnd = jo.get("enderecoFrontEnd").toString();

            System.out.println(ea.portaFrontEnd);
            System.out.println(ea.enderecoFrontEnd);
            System.out.println(ea.enderecoDiretorio);
            System.out.println(ea.portaDiretorio);

            return ea;
        }
        catch(Exception e){
            System.out.println(e);
            return null;
        }
        

    }

    public static int little2big(int i) {
        return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
    }

    /**
     * Devolve null senao estiver autenticado
     * Devolve o papel "empresa" ou "cliente" consoante o papel do utilizador
     */
    public static RespostaAutenticacao leMensagemInicial(CodedInputStream cis){
        try{
            int len = cis.readRawLittleEndian32();
            len = little2big(len);
            byte[] ba = cis.readRawBytes(len);
            RespostaAutenticacao resposta = RespostaAutenticacao.parseFrom(ba);
            
            boolean sucesso = resposta.getSucesso();
            if( sucesso == true){
                return resposta;
                // String papel = resposta.getPapel();
                // return papel;
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

            cos.writeSFixed32NoTag(little2big(ba.length));
            cos.writeRawBytes(ba);
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
            
            if(args.length==0){
                System.out.println("Tem de fornecer as portas!");
                return;
            }

            enderecos = parseEnderecos(args[0]);
            if(enderecos==null){
                System.out.println("Erro no parse!");
                return;
            }

            
            Socket s = new Socket(enderecos.enderecoFrontEnd,Integer.parseInt(enderecos.portaFrontEnd));


            /* ------------------------------ AUTENTICACAO/REGISTO -------------------------------- */

            boolean autenticado = false;
            boolean sair = false;
            BufferedReader inP = new BufferedReader(new InputStreamReader(System.in));

            CodedInputStream cis = CodedInputStream.newInstance(s.getInputStream());
            CodedOutputStream cos = CodedOutputStream.newInstance(s.getOutputStream());
            
            while(!autenticado && !sair){
                System.out.println("1 - Autenticar");
                System.out.println("2 - Visualizar Leilões Ativos");
                System.out.println("Outro para sair");

                System.out.print("Opção: ");
                int opcao = 3;

                try{
                    opcao = Integer.parseInt(inP.readLine());
                } catch(Exception e){
                    System.out.println(e);
                }
                
                String user = null;
                RespostaAutenticacao resposta = null;

                
                switch(opcao){
                    //Se calhar só vamos buscar o resposta se o user nao for nulo (poed acontecer se der uma exceçao)
                    case 1: user = autenticaCliente(inP,cos); resposta = leMensagemInicial(cis); break;
                    case 2: enderecos.leiloesAtivos(); break;
                    default: sair = true; System.out.println("bye!"); break;
                }
                
                autenticado = true;
                
                if(user==null || resposta==null){
                    autenticado = false;
                }
                else{
                    String papel = resposta.getPapel();
                    boolean leilao = resposta.hasLeilaoSubscrito() ? resposta.getLeilaoSubscrito() : false;
                    boolean emissao = resposta.hasEmissaoSubscrita() ? resposta.getEmissaoSubscrita() : false;
                    List<String> emps = resposta.getEmpresasSubscritasList();
                    switch(papel){
                        case "empresa": autenticado=true; (new Empresa(user, s, enderecos, leilao, emissao, emps)).menuInicial(); System.out.println("bye!"); break; //mandar para a empresa
                        case "licitador": autenticado=true; (new Licitador(user, s, enderecos, leilao, emissao, emps)).menuInicial(); System.out.println("bye!"); break; //mandar para o licitador
                        default: break;
                    }
                }

            }
        }
        finally{
            sc.close();
        }
        return; 
    }  
}
