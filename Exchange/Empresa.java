import java.time.LocalDateTime;
import java.util.TreeSet;

public class Empresa{
    private int idEmissao = 0;
    private int idLeilao = 0;
    public String nome;
    private Leilao leilaoCurso;
    private TreeSet<Leilao> leiloesEfetuados = new TreeSet<>();
    private Emissao emissaoCurso;
    private TreeSet<Emissao> emissoesEfetuadas = new TreeSet<>();
    
    public Empresa(String nome){
        this.nome = nome;
    }

    public boolean criarLeilao(int montante, float taxa, LocalDateTime fim){
        //para criar um novo leilao
        if(leilaoCurso == null){
            leilaoCurso = new Leilao(idLeilao++,montante,taxa,fim);
            return true;
        }
        return false;
    }

    public boolean criarEmissao(int montante, LocalDateTime fim){
        //para criar uma nova emissao
        if(emissaoCurso == null){
            //como definir a taxa? ver nas duvidas ----------------------------FALTA
            int taxa = 0;
            emissaoCurso = new Emissao(idEmissao++, montante, taxa, fim);
            return true;
        }
        return false;
    }

    public boolean licitaLeilao(int id, String cliente, int montante, float taxa){
        //para licitar um leilao (o id será necessário?)
        if(id == leilaoCurso.id){
            return leilaoCurso.licita(cliente, montante, taxa);
        }
        else{
            //se calhar aqui podia ser uma excecao 
            //para dizer que o leilao ja acabou
            return false;
        }
    }

    public Emissao licitaEmissao(int id, String cliente, int montante){
        //para licitar um leilao (o id será necessário?)
        if(id == emissaoCurso.id){
            if(!emissaoCurso.licita(cliente, montante)){
                return terminaEmissao();
            }
            return null;
        }
        else{
            //aqui tem de ser uma excecao 
            //para dizer que a emissao ja acabou
            return null;
        }
    }

    public Leilao terminaLeilao(){
        //termina o leilao
        leilaoCurso.termina();
        leiloesEfetuados.add(leilaoCurso); //podiamos dividir em com sucesso e sem sucesso???
        l = leilaoCurso;
        leilaoCurso = null;
        return l;
    }

    public Emissao terminaEmissao(){
        //termina a emissao em curso
        emissaoCurso.termina();
        emissoesEfetuadas.add(emissaoCurso);
        e = emissaoCurso;
        emissaoCurso = null;
        return e;
    }
}