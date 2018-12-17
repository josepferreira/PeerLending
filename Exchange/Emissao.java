import java.time.LocalDateTime;

public class Emissao extends Emprestimo{
    

    public Emissao(int id, int montante, float taxa, LocalDateTime fim){
        super(id, montante, taxa, fim);
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