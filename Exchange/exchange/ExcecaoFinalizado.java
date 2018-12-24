package exchange;

public class ExcecaoFinalizado extends Exception{
    public String empresa;
    public String mensagem;
    
    public ExcecaoFinalizado(String e, String msg){
        super();
        empresa = e;
        mensagem = msg;
    }

}