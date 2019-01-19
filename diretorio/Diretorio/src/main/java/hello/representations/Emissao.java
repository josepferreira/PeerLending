package hello.representations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeSet;

public class Emissao {

    public int id;

    public String empresa;

    public long montante;
    public float taxa;
    public ArrayList<Proposta> propostas;// = new TreeSet<>(); //pode estar ordenado ao contrário, ou seja a melhor proposta está no fim

    public boolean terminado = false;
    public boolean sucesso = false;
    public String fim;
    @JsonCreator
    public Emissao (@JsonProperty("id") int id, @JsonProperty("empresa") String empresa,
                   @JsonProperty("propostas") ArrayList<Proposta> p,
                   @JsonProperty("montante") long montante, @JsonProperty("taxa") float taxa,
                    @JsonProperty("fim") String fim, @JsonProperty("sucesso") boolean sucesso) {
        System.out.println("Cheguei ao contrutor com 3 no leilao");
        System.out.println("Propotas");
        System.out.println(p);
        this.id = id;
        this.empresa = empresa;
        this.propostas = p;
        this.montante = montante;
        this.taxa = taxa;
        this.fim = fim;
        this.sucesso = sucesso;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leilao leilao = (Leilao) o;
        return this.id == leilao.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Leilao{" +
                "id=" + id +
                ", empresa='" + empresa + '\'' +
                ", terminado=" + terminado +
                '}';
    }
}
