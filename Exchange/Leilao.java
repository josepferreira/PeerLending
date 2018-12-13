import java.time.LocalDateTime;
import java.util.ArrayList;

public class Leilao{
    private int id;
    private int ultimaProposta = 0;
    private int montante;
    private float taxaMaxima;
    private TreeSet<Proposta> propostas = new ArrayList<>();
    private boolean terminado = false;
    private boolean sucesso;
    private LocalDateTime fim;

    public Leilao(int id, int montante, float taxa, LocalDateTime fim){
        this.id = id;
        this.montante = montante;
        this.taxaMaxima = taxa;
        this.fim = fim;
    }

    private Proposta adicionaProposta(Proposta p){
        //metodo que verifica se a proposta vai ficar colocada, em caso afirmativo adiciona a mesma
        //retorna a proposta que foi removida, ou null no caso de nenhuma ter sido
        //caso a proposta nao seja adicionada retorna-a
        return null;
    }

    public Proposta licita(String cliente, int montante, float taxa){
        //faz uma licitacao ao leilao
        //caso seja adicionado ao leilao e outro seja removida tem de avisar o que foi removido
        Proposta p = new Proposta(ultimaProposta++,cliente, montante, taxa);
        return adicionaProposta(p);
    }

    public boolean termina(){
        //termina o respetivo leilao
        terminado = true;
        int montanteAmealhado = propostas.stream()
                                    .mapToInt(p -> p.montante)    
                                    .sum();
        sucesso = (montanteAmealhado >= montante ? true : false);
        propostas.get(prpostas.size()-1).montante -= (montanteAmealhado - montante);
        return sucesso;
    }
}