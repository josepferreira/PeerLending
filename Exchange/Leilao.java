import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Leilao extends Emprestimo{
    private boolean sucesso;
    
    public Leilao(int id, int montante, float taxa, LocalDateTime fim){
        super(id, montante, taxa, fim);
    }

    private boolean verificaUltrapassados()
        throws ExcecaoUltrapassado{
        int montanteAmealhado = 0;
        ArrayList<Proposta> clientes = new ArrayList<>();
        for(Proposta p: propostas.descendingSet()){
            if(montanteAmealhado > montante){
                clientes.add(p);
            }
            else{
                montanteAmealhado += p.montante;
            }
        }
        
        if(clientes.size() != 0){
            propostas.removeAll(clientes);
            throw new ExcecaoUltrapassado(propostas.first(),clientes,"A sua proposta foi ultrapassada!");
        }

        return true;
    }

    private int montanteAmealhado(){
        return propostas.stream()
                .mapToInt(a -> a.montante)
                .sum();
    }   

    private boolean adicionaProposta(Proposta p)
        throws ExcecaoUltrapassado{
        //metodo que verifica se a proposta vai ficar colocada, em caso afirmativo adiciona a mesma
        //retorna a proposta que foi removida, ou null no caso de nenhuma ter sido
        //caso a proposta nao seja adicionada retorna-a
        if(this.montanteAmealhado() < montante){
            propostas.add(p);
            return verificaUltrapassados();
        }
        else{
            if(p.compareTo(propostas.first()) < 0){
                return false;
            }
            propostas.add(p);
            return verificaUltrapassados();
        }
    }

    public boolean licita(String cliente, int montante, float taxa)
        throws ExcecaoUltrapassado{
        //faz uma licitacao ao leilao
        //caso seja adicionado ao leilao e outro seja removida tem de avisar o que foi removido
        if(taxa > this.taxa){
            return false;
        }

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
        if(sucesso){
            int diferenca = montanteAmealhado - montante;
            propostas.first().montante -= diferenca;
        }
        return sucesso;
    }
}