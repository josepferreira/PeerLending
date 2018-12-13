import java.time.LocalDateTime;

public class Emissao{
    private int id;
    private int montante;
    private float taxa;
    private ArrayList<Proposta> propostas = new ArrayList<>();
    private boolean terminado = false;
    private boolean sucesso;
    private LocalDateTime fim;

    public Emissao(int id, int montante, float taxa, LocalDateTime fim){
        this.id = id;
        this.montante = montante;
        this.taxa = taxa;
        this.fim = fim;
    }

    public boolean licita(String cliente, int montante){
        //faz uma licitacao ao Emissao
    }

    public boolean termina(){
        //termina o respetivo Emissao
    }
}