import java.time.LocalDateTime;
import java.util.TreeSet;

public class Empresa{
    private int idEmprestimo = 0;
    public String nome;
    private Emprestimo emprestimoCurso;
    private TreeSet<Leilao> leiloesEfetuados = new TreeSet<>();
    private TreeSet<Emissao> emissoesEfetuadas = new TreeSet<>();
    
    public Empresa(String nome){
        this.nome = nome;
    }

    public boolean criarLeilao(int montante, float taxa, LocalDateTime fim){
        //para criar um novo leilao
        if(emprestimoCurso == null){
            emprestimoCurso = new Leilao(idLeilao++,montante,taxa,fim);
            return true;
        }
        return false;
    }

    public boolean criarEmissao(int montante, LocalDateTime fim){
        //para criar uma nova emissao
        if(emprestimoCurso == null){
            //como definir a taxa? ver nas duvidas ----------------------------FALTA
            int taxa = 0;
            emprestimoCurso = new Emissao(idEmissao++, montante, taxa, fim);
            return true;
        }
        return false;
    }

    public boolean licitaLeilao(int id, String cliente, int montante, float taxa) 
    throws ExcecaoUltrapassado, ExcecaoFinalizado{
        //para licitar um leilao (o id será necessário?)
        if(ememprestimoCurso == null){
            throw new  ExcecaoFinalizado(nome, "O leilão pretendido já não se encontra ativo!");
        }
        if(!(emprestimoCurso instanceof Leilao)){
            return false;
        }
        if(id == emprestimoCurso.id){
            return ((Leilao)emprestimoCurso).licita(cliente, montante, taxa);
        }
        else{
            throw new  ExcecaoFinalizado(nome, "O leilão pretendido já não se encontra ativo!");
        }
    }

    public Emissao licitaEmissao(int id, String cliente, int montante){
        //para licitar uma emissao (o id será necessário?)
        if(!(emprestimoCurso instanceof Emissao)){
            return null;
        }

        if(id == emissaoCurso.id){
            if(!((Emissao)emissaoCurso).licita(cliente, montante)){
                return (Emissao)terminaEmissao();
            }
            return null;
        }
        else{
            //aqui tem de ser uma excecao 
            //para dizer que a emissao ja acabou
            return null;
        }
    }

    public Emprestimo terminaEmprestimo(){
        emprestimoCurso.termina();
        Emprestimo e = emprestimoCurso;
        if(e instanceof Leilao){
            leiloesEfetuados.add((Leilao)e);
        }
        else{
            emissoesEfetuadas.add((Emissao)e);
        }
        emprestimoCurso = null;
        return e;
    }
}