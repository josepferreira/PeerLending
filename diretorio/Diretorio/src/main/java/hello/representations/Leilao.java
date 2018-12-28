package hello.representations;

import com.fasterxml.jackson.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

public class Leilao {
    public int id;
    public String empresa;
    //public long montante;
    //public float taxa;
    //public TreeSet<Proposta> propostas = new TreeSet<>(); //pode estar ordenado ao contrário, ou seja a melhor proposta está no fim
    public boolean terminado = false;
    //public LocalDateTime fim;
    @JsonCreator
    public Leilao(@JsonProperty("id") int id, @JsonProperty("empresa") String empresa) {
        this.id = id;
        this.empresa = empresa;
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
