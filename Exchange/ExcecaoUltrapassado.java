import java.util.ArrayList; 

public class ExcecaoUltrapassado extends Exception{
    public String empresa;
    public Proposta proposta;
    public ArrayList<Proposta> propostas;
    public String mensagem;
    
    public ExcecaoUltrapassado(Proposta p, ArrayList<Proposta> c, String msg){
        super();
        proposta = p;
        propostas = c;
        mensagem = msg;
    }

}