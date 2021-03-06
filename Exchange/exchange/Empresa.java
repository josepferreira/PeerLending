package exchange;

import java.time.LocalDateTime;
import java.util.TreeSet;

public class Empresa{
    private int idEmprestimo = 0;
    public String nome;
    private Emprestimo emprestimoCurso;
    private TreeSet<Leilao> leiloesEfetuados = new TreeSet<>();
    private TreeSet<Emissao> emissoesEfetuadas = new TreeSet<>();
    
    public Empresa(String nome){
        this.nome = nome;
    }

    public Emprestimo criarLeilao(long montante, float taxa, LocalDateTime fim){
        //para criar um novo leilao
        if(taxa < 0){
            return null;
        }
        if(emprestimoCurso == null){
            emprestimoCurso = new Leilao(idEmprestimo++,this.nome,montante,taxa,fim);
            return emprestimoCurso;
        }
        return null;
    }

    public Emprestimo criarEmissao(long montante, LocalDateTime fim) throws ExcecaoIndisponivel{
        //para criar uma nova emissao
        if(emprestimoCurso == null){
            if(leiloesEfetuados.stream()
                    .filter(a -> a.sucesso)
                    .count()==0){
                //da excecao a dizer q n pode criar
                throw new ExcecaoIndisponivel(nome,"Não pode criar uma emissao por ainda não ter um leilao efetuado com sucesso!");
            }
            //como definir a taxa? ver nas duvidas ----------------------------FALTA
            float taxa = 0;
            if(emissoesEfetuadas.size()==0){
                taxa = leiloesEfetuados.first().taxaMaxima();
                if(!leiloesEfetuados.first().sucesso){
                    taxa *= 1.1;
                }
            }
            else{
                if(leiloesEfetuados.first().id > emissoesEfetuadas.first().id){
                    //taxa do leilao
                    taxa = leiloesEfetuados.first().taxaMaxima();
                    if(!leiloesEfetuados.first().sucesso){
                        taxa *= 1.1;
                    }
                }
                else{
                    //taxa da emissao
                    taxa = emissoesEfetuadas.first().taxa;
                    if(!emissoesEfetuadas.first().sucesso()){
                        taxa *= 1.1;
                    }
                }
            }
            emprestimoCurso = new Emissao(idEmprestimo++, this.nome,montante, taxa, fim);
            return emprestimoCurso;
        }
        return null;
    }

    public boolean licitaLeilao(String cliente, long montante, float taxa) 
    throws ExcecaoUltrapassado, ExcecaoFinalizado{
        //para licitar um leilao (o id será necessário?)
        if(emprestimoCurso == null){
            throw new  ExcecaoFinalizado(nome, "O leilão pretendido já não se encontra ativo!");
        }
        if(!(emprestimoCurso instanceof Leilao)){
            return false;
            //pode dar excecao a dizer q leilao n existe
        }
        return ((Leilao)emprestimoCurso).licita(cliente, montante, taxa);
        
    }

    //retorna a Emissao caso tenha terminado, null caso contrario
    public Emissao licitaEmissao(String cliente, long montante)
        throws ExcecaoFinalizado{
        //para licitar uma emissao (o id será necessário?)
        System.out.println("Licitar emissao para a empresa: " + nome);
        if(emprestimoCurso == null){
            throw new  ExcecaoFinalizado(nome, "A emissão pretendida já não se encontra ativa!");
        }

        System.out.println("Vou licittar!");

        if(!(emprestimoCurso instanceof Emissao)){
            return null;
            //pode dar excecao a dizer q emissao nao existe
        }

        
        if(!((Emissao)emprestimoCurso).licita(cliente, montante)){
            return (Emissao)terminaEmprestimo();
        }

        return null;
        
    }

    public Emprestimo terminaEmprestimo(int id){
        System.out.println("Terminar emprestimo!");
        if(emprestimoCurso == null){
            System.out.println("Não terminar emprestimo, é null!");
            return null;
        }

        if(id != emprestimoCurso.id){
            System.out.println("Não terminar emprestimo, ids diferentes!");
            return null;
        }

        return terminaEmprestimo();

    }

    private Emprestimo terminaEmprestimo(){
        if(emprestimoCurso == null){
            return null;
        }

        emprestimoCurso.termina();
        Emprestimo e = emprestimoCurso;
        if(e instanceof Leilao){
            leiloesEfetuados.add((Leilao)e);
        }
        else{
            emissoesEfetuadas.add((Emissao)e);
        }
        emprestimoCurso = null;
        return e;
    }

}