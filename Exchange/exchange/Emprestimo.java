package exchange;

import java.util.TreeSet; 
import java.time.LocalDateTime;

public abstract class Emprestimo{

    public int id;
    public String empresa;
    int ultimaProposta = 0;
    long montante;
    float taxa;
    TreeSet<Proposta> propostas = new TreeSet<>(); //pode estar ordenado ao contrário, ou seja a melhor proposta está no fim
    boolean terminado = false;
    LocalDateTime fim;

    public Emprestimo(int id, String empresa, long montante, float taxa, LocalDateTime fim){
        this.id = id;
        this.empresa = empresa;
        this.montante = montante;
        this.taxa = taxa;
        this.fim = fim;
    }

    public abstract boolean termina();

    public abstract boolean equals(Object o);
}