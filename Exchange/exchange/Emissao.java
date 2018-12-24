package exchange;

import java.time.LocalDateTime;

public class Emissao extends Emprestimo{
    

    public Emissao(int id, String empresa, long montante, float taxa, LocalDateTime fim){
        super(id, empresa, montante, taxa, fim);
    }

    //retorna true se for aceite, false se for aceita e terminar a emissao
    public boolean licita(String cliente, long montante){
        //faz uma licitacao ao Emissao
        long montanteAmealhado = propostas.stream()
                                    .mapToLong(p -> p.montante)    
                                    .sum();
        long diferenca = this.montante - montanteAmealhado;
        
        if(diferenca < montante){
            montante = diferenca;
        }

        Proposta p = new Proposta(ultimaProposta++, cliente, montante, taxa);

        propostas.add(p);
        if(diferenca <= montante){
            return false;
        }
        return true;
    }

    public boolean termina(){
        if(terminado == true){
            return false;
        }
        
        terminado = true;
        long montanteAmealhado = propostas.stream()
                                    .mapToLong(p -> p.montante)    
                                    .sum();
        long diferenca = montanteAmealhado - montante;
        propostas.first().montante -= (diferenca > 0 ? diferenca : 0);
        return true;
        //termina o respetivo Emissao
    }

    public boolean equals(Object o){
        if(o==null) return false;

        if(!(o instanceof Emissao)) return false;

        Emissao e = (Emissao)o;

        return ((e.id == this.id) && (e.empresa.equals(this.empresa)));
    }
}