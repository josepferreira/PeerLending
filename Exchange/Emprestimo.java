public abstract class Emprestimo{

    public int id;
    int ultimaProposta = 0;
    int montante;
    float taxa;
    TreeSet<Proposta> propostas = new TreeSet<>();
    boolean terminado = false;
    LocalDateTime fim;

    public Emprestimo(int id, int montante, float taxa, LocalDateTime fim){
        this.id = id;
        this.montante = montante;
        this.taxa = taxa;
        this.fim = fim;
    }

    public abstract boolean termina();
}