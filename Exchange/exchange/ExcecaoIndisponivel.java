package exchange;

public class ExcecaoIndisponivel extends Exception{
    public String empresa;
    public String mensagem;
    
    public ExcecaoIndisponivel(String e, String msg){
        super();
        empresa = e;
        mensagem = msg;
    }

}