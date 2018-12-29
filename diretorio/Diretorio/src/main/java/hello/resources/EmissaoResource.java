package hello.resources;

import hello.representations.Emissao;
import hello.representations.Leilao;

import com.google.common.base.Optional;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;

@Path("/emissao")
@Produces(MediaType.APPLICATION_JSON)
public class EmissaoResource {
    private HashMap<String, ArrayList<Emissao>> emissoesAtivas = new HashMap<>();
    private HashMap<String, ArrayList<Emissao>> emissoesFinalizadas = new HashMap<>();

    public EmissaoResource(){

        System.out.println("Fui criada!");
    }

    //Emissoes ativas
    @GET
    public ArrayList<Emissao> getEmissoesAtivasEmpresa(@QueryParam("empresa") Optional<String> empresa) {

        if(!empresa.isPresent()) {

            ArrayList<Emissao> emissoes = new ArrayList<>();

            for(ArrayList<Emissao> aux: emissoesAtivas.values()){
                emissoes.addAll(aux);
            }
            return emissoes;
        }

        return emissoesAtivas.get(empresa.get());
    }

    //emissoes Finalizadas
    @GET
    @Path("/finalizadas")
    public ArrayList<Emissao> getLeiloesFinalizados(@QueryParam("empresa") Optional<String> empresa){
        if(!empresa.isPresent()) {
            ArrayList<Emissao> emissoes = new ArrayList<>();
            for (ArrayList<Emissao> aux : emissoesFinalizadas.values()) {
                emissoes.addAll(aux);
            }
            return emissoes;
        }

        return emissoesFinalizadas.get(empresa.get());
    }



    /**
     * Para a Exchange
     */
    @PUT
    @Path("/{empresa}/terminado/{id}")

    public Response terminaEmissao(@PathParam("empresa") String empresa, @PathParam("id") int id) {

        ArrayList<Emissao> emissoesA = emissoesAtivas.get(empresa);
        Emissao emissao = emissoesA.stream().filter(l -> l.id == id). findFirst().get();
        System.out.println(emissao);
        emissao.terminado = true;

        ArrayList<Emissao> emissaoF = emissoesFinalizadas.get(empresa);
        if(emissaoF == null)
            emissaoF = new ArrayList<Emissao>();
        emissaoF.add(emissao);
        boolean continua = true;
        for(int i=0; i<emissoesA.size() && continua; i++){
            if(emissoesA.get(i).equals(emissao)) {
                emissoesA.remove(i);
                continua = false;
            }
        }

        emissoesAtivas.put(empresa, emissoesA);
        emissoesFinalizadas.put(empresa, emissaoF);

        System.out.println(emissoesAtivas.get(empresa));
        System.out.println(emissoesFinalizadas.get(empresa));

        return Response.ok(emissao).build();
    }

    @POST
    //@Consumes(MediaType.APPLICATION_JSON)
    public Response add(@FormParam("id") int id, @FormParam("empresa") String empresa) {
        Emissao emissao = new Emissao(id, empresa);
        if(emissoesAtivas.containsKey(empresa)){
            ArrayList<Emissao> l = emissoesAtivas.get(empresa);
            l.add(emissao);
            emissoesAtivas.put(empresa, l);
        }
        else{
            ArrayList<Emissao> l = new ArrayList<>();
            l.add(emissao);
            emissoesAtivas.put(empresa, l);
        }
        return Response.ok(emissao).build();
    }

}

