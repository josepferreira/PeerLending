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
            montanteAmealhado += p.montante;

            if(montanteAmealhado > montante){
                clientes.add(p);
            }
        }

        propostas.removeAll(clientes);
        

        if(clientes.size() != 0){
            ArrayList<String> cl = (ArrayList<String>)clientes.stream()
                                    .map(a -> a.cliente)
                                    .collect(Collectors.toList());
            throw new ExcecaoUltrapassado(propostas.first(),cl,"A sua proposta foi ultrapassada!");
        }

        return false;
    }

    private boolean adicionaProposta(Proposta p, String cliente)
        throws ExcecaoUltrapassado{
        //metodo que verifica se a proposta vai ficar colocada, em caso afirmativo adiciona a mesma
        //retorna a proposta que foi removida, ou null no caso de nenhuma ter sido
        //caso a proposta nao seja adicionada retorna-a
        if(this.montanteAmealhado() < montante){
            propostas.add(p);
            return verificaUltrapassados();
        }
        else{
            for(Proposta aux : propostas){
                if(p.compareTo(aux) > 0){
                    propostas.add(p);
                    return verificaUltrapassados();
                }
                else{
                    ArrayList<String> cl = new ArrayList<String>();
                    cl.add(cliente);
                    throw new ExcecaoUltrapassado(aux,cl,"A sua proposta não foi considerada!");   
                }
            }
        }
        return false;
    }

    public boolean licita(String cliente, int montante, float taxa)
        throws ExcecaoUltrapassado{
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
        int diferenca = montanteAmealhado - montante;
        propostas.get(propostas.size()-1).montante -= (diferenca > 0 ? diferenca : 0);
        return sucesso;
    }
}