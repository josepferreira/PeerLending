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
    }

    //Leiloes ativos
    @GET
    public ArrayList<Leilao> getLeiloesAtivosEmpresa(@QueryParam("empresa") Optional<String> empresa) {
        if(!empresa.isPresent()) {
            ArrayList<Leilao> leiloes = new ArrayList<>();
            for(ArrayList<Leilao> aux: leiloesAtivos.values()){
                leiloes.addAll(aux);
            }
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
    public Response terminaLeilao(@PathParam("empresa") String empresa, @PathParam("id") int id) {
        ArrayList<Leilao> leiloesA = leiloesAtivos.get(empresa);
        Leilao leilao = leiloesA.stream().filter(l -> l.id == id). findFirst().get();
        System.out.println(leilao);
        leilao.terminado = true;

        ArrayList<Leilao> leiloesF = leiloesFinalizados.get(empresa);
        if(leiloesF == null)
            leiloesF = new ArrayList<Leilao>();
        leiloesF.add(leilao);
        boolean continua = true;
        for(int i=0; i<leiloesA.size() && continua; i++){
            if(leiloesA.get(i).equals(leilao)) {
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

    @POST
    //@Consumes(MediaType.APPLICATION_JSON)
    public Response add(@FormParam("id") int id, @FormParam("empresa") String empresa) {
        Leilao leilao = new Leilao(id, empresa);
        if(leiloesAtivos.containsKey(empresa)){
            ArrayList<Leilao> l = leiloesAtivos.get(empresa);
            l.add(leilao);
            leiloesAtivos.put(empresa, l);
        }
        else{
            ArrayList<Leilao> l = new ArrayList<>();
            l.add(leilao);
            leiloesAtivos.put(empresa, l);
        }
        return Response.ok(leilao).build();
    }

}

