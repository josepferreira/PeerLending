package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import org.zeromq.ZMQ;

/**
 * Ira apresentar o menu das subscricoes e será responsável em comunicar com a classe Notificacaoes
 */
public class GerirSubscricoes{
    
    boolean leiloesSubscritos = false;
    boolean emissoesSubscritas = false;
    HashSet<String> empresasSubscritas = new HashSet<>();
    BufferedReader inP = new BufferedReader(new InputStreamReader(System.in));
    ZMQ.Socket socket;


    public GerirSubscricoes(ZMQ.Context context){
        this.socket = context.socket(ZMQ.PUSH);
        this.socket.bind("inproc://notificacoes");
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
    }

    public void removeEmpresa(String empresa){
        empresasSubscritas.remove(empresa);
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
            System.out.print("Opção: ");
            
            try{
                opcao = Integer.parseInt(inP.readLine());
                
                switch(opcao){
                    case 1:
                        if(leiloesSubscritos == false){
                            System.out.println("Vou subscrever todos os leilões");
                            leiloesSubscritos = true;
                        }
                        continua = false;
                        break;
                    case 2:
                        if(emissoesSubscritas == false){
                            System.out.println("Vou subscrever todos as emissões");
                            emissoesSubscritas = true;
                        }
                        continua = false;
                        break;
                    case 3:
                        System.out.print("Escreva o nome da empresa: ");
                        String empresa = inP.readLine();
                        if(!empresasSubscritas.contains(empresa)){
                            System.out.println("Vou mandar uma empresa para subscrever! " + empresa);
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
                case 1: apresentaListaSubscricao(); break;
                case 2: acrescentaSubscricao(); break;
                //case 3: retiraSubscricao(); break;
                default: continua = false;
            }
        }

    }



}