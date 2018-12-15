import java.time.LocalDateTime;

public class Emissao{
    public int id;
    private int ultimaProposta = 0;
    private int montante;
    private float taxa;
    private TreeSet<Proposta> propostas = new TreeSet<>();
    private boolean terminado = false;
    private LocalDateTime fim;

    public Emissao(int id, int montante, float taxa, LocalDateTime fim){
        this.id = id;
        this.montante = montante;
        this.taxa = taxa;
        this.fim = fim;
    }

    private boolean possoTerminar(){
        int montanteAmealhado = propostas.stream()
                                    .mapToInt(p -> p.montante)    
                                    .sum();
        return (montanteAmealhado >= montante);
    }

    public boolean licita(String cliente, int montante){
        //faz uma licitacao ao Emissao
        Proposta p = new Proposta(ultimaProposta++, cliente, montante);
        if(possoTerminar()){
            termina();
            return false;
        }
        return true;
    }

    public boolean termina(){
        terminado = true;
        int montanteAmealhado = propostas.stream()
                                    .mapToInt(p -> p.montante)    
                                    .sum();
        int diferenca = montanteAmealhado - montante;
        propostas.get(propostas.size()-1).montante -= (diferenca > 0 ? diferenca : 0);
        return true;
        //termina o respetivo Emissao
    }
}