import java.util.ArrayList; 

public class ExcecaoUltrapassado extends Exception{
    public String empresa;
    public Proposta proposta;
    public ArrayList<String> cliente;
    public String mensagem;
    
    public ExcecaoUltrapassado(Proposta p, ArrayList<String> c, String msg){
        super();
        proposta = p;
        cliente = c;
        mensagem = msg;
    }

}