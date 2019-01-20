package hello.resources;

import hello.representations.Leilao;
import hello.representations.Saying;

import com.google.common.base.Optional;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;

@Path("/leilao")
@Produces(MediaType.APPLICATION_JSON)
public class LeilaoResource {
    /*private final String template;
    private volatile String defaultName;
    private long counter;*/
    private HashMap<String, ArrayList<Leilao>> leiloesAtivos = new HashMap<>();
    private HashMap<String, ArrayList<Leilao>> leiloesFinalizados = new HashMap<>();

    public LeilaoResource(){
        System.out.println("Fui criada!");
        ArrayList<Leilao> leiloes = new ArrayList<>();
        //leiloes.add(new Leilao(1, "ola", new ArrayList<>(),10,(float)0.5));
        //leiloes.add(new Leilao(2, "ola", new ArrayList<>(),11,(float)0.6));
        //leiloes.add(new Leilao(3, "mania", new ArrayList<>(), 11, (float)1.5));
        leiloesAtivos.put("ola", leiloes);

    }

    //Leiloes ativos
    @GET
    public ArrayList<Leilao> getLeiloesAtivosEmpresa(@QueryParam("empresa") Optional<String> empresa) {
        if(!empresa.isPresent()) {
            ArrayList<Leilao> leiloes = new ArrayList<>();
            for(ArrayList<Leilao> aux: leiloesAtivos.values()){
                leiloes.addAll(aux);
            }
            System.out.println(leiloes);
            return leiloes;
        }

        return leiloesAtivos.get(empresa.get());
    }

    //leiloes Finalizados
    @GET
    @Path("/finalizados")
    public ArrayList<Leilao> getLeiloesFinalizados(@QueryParam("empresa") Optional<String> empresa){
        if(!empresa.isPresent()) {
            ArrayList<Leilao> leiloes = new ArrayList<>();
            for (ArrayList<Leilao> aux : leiloesFinalizados.values()) {
                leiloes.addAll(aux);
            }
            return leiloes;
        }

        return leiloesFinalizados.get(empresa.get());
    }



    /**
     * Para a Exchange
     */
    @PUT
    @Path("/{empresa}/terminado/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response terminaLeilao(@PathParam("empresa") String empresa, @PathParam("id") int id, Leilao leilaoA) {
        System.out.println("Cheguei ao put leilao");
        ArrayList<Leilao> leiloesA = leiloesAtivos.get(empresa);
        System.out.println(leilaoA);
        if(leiloesA == null){
            //dar erro
            return Response.status(Response.Status.NOT_FOUND).entity("Não existem leiloe ativos para a empresa: " + empresa).build();
        }
        if(leilaoA.id != id){
            return Response.status(Response.Status.NOT_FOUND).entity("Erro nos ids dos leiloes que esta a tentar alterar: " + empresa).build();
        }
        try {
            Leilao leilao = leiloesA.stream().filter(l -> l.id == id).findFirst().get();

            System.out.println(leilao);
            leilao.terminado = true;
            leilao.propostas = leilaoA.propostas;
            leilao.sucesso = leilaoA.sucesso;

            ArrayList<Leilao> leiloesF = leiloesFinalizados.get(empresa);
            if (leiloesF == null)
                leiloesF = new ArrayList<Leilao>();
            leiloesF.add(leilao);
            boolean continua = true;
            for (int i = 0; i < leiloesA.size() && continua; i++) {
                if (leiloesA.get(i).equals(leilao)) {
                    leiloesA.remove(i);
                    continua = false;
                }
            }

            leiloesAtivos.put(empresa, leiloesA);
            leiloesFinalizados.put(empresa, leiloesF);

            System.out.println(leiloesAtivos.get(empresa));
            System.out.println(leiloesFinalizados.get(empresa));

            return Response.ok(leilao).build();
        }
        catch(Exception exc){
            return Response.status(Response.Status.NOT_FOUND).entity("O leilão referido não se encontra ativa").build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(Leilao leilao) {
        System.out.println("Cheguei");
        if(leiloesAtivos.containsKey(leilao.empresa)){
            ArrayList<Leilao> l = leiloesAtivos.get(leilao.empresa);
            l.add(leilao);
            leiloesAtivos.put(leilao.empresa, l);
        }
        else{
            ArrayList<Leilao> l = new ArrayList<>();
            l.add(leilao);
            leiloesAtivos.put(leilao.empresa, l);
        }

        return Response.ok(leilao).build();
    }

}

