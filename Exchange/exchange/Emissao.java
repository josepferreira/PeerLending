package exchange;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.json.*;

public class Emissao extends Emprestimo implements Comparable{
    

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
        if(!propostas.isEmpty()){
            propostas.first().montante -= (diferenca > 0 ? diferenca : 0);
        }
        return true;
        //termina o respetivo Emissao
    }

    public boolean equals(Object o){
        if(o==null) return false;

        if(!(o instanceof Emissao)) return false;

        Emissao e = (Emissao)o;

        return ((e.id == this.id) && (e.empresa.equals(this.empresa)));
    }

    public boolean sucesso(){
        long montanteAmealhado = propostas.stream()
                                    .mapToLong(p -> p.montante)    
                                    .sum();
        return montanteAmealhado >= montante;
    }

    public int compareTo(Object o){
        Emissao e = (Emissao)o;

        if(this.id > e.id){
            return 1;
        }
        if(this.id < e.id){
            return -1;
        }
        return 0;
    }

    public String getJSON(){
        // podemos depois ter uma funcao que converte leilao em json 
        // e uma que converte json em leilao

        try{
            JSONObject jo = new JSONObject();

        jo.put("id",id);
        jo.put("empresa",empresa);
        jo.put("montante",montante);
        jo.put("taxa",taxa);
        jo.put("fim",fim.toString());
        jo.put("propostas",propostas.stream()
                .map(p -> p.getJSON())
                .collect(Collectors.toList()));
        
 
        return jo.toString();
        }
        catch(Exception e){
            return "";
        }
    }

    public String toString(){
        String res = "Emissao: {";
        res += "id: " + id;
        res += "; empresa: " + empresa;
        res += "; montante total: " + montante;
        res += "; taxa: " + taxa;
        res += "; propostas: " + propostas.toString() + "}";

        return res;
    }
}