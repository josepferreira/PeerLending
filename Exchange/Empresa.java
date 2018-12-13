import java.util.ArrayList;

public class Empresa{
    private int idEmissao = 0;
    private int idLeilao = 0;
    private String nome;
    private Leilao leilaoCurso;
    private ArrayList<Leilao> leiloesEfetuados = new ArrayList<>();
    private Emissao emissaoCurso;
    private ArrayList<Emissao> emissoesEfetuados = new ArrayList<>();
    
    public Empresa(String nome){
        this.nome = nome;
    }

    public boolean criarLeilao(int montante, float taxa){
        //para criar um novo leilao
    }

    public boolean criarEmissao(int montante){
        //para criar uma nova emissao
    }

    public boolean licitaLeilao(int id, String cliente, int montante, float taxa){
        //para licitar um leilao (o id será necessário?)
    }

    public boolean licitaEmissao(int id, String cliente, int montante){
        //para licitar um leilao (o id será necessário?)
    }

    public Leilao terminaLeilao(){
        //termina o leilao
    }

    public Emissao terminaEmissao(){
        //termina a emissao em curso
    }
}