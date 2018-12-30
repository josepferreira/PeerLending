package hello.resources;

import com.google.common.base.Optional;
import hello.representations.Exchange;
import hello.representations.Leilao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Path("/exchange")
@Produces(MediaType.APPLICATION_JSON)
public class ExchangeResource {

    HashMap<String, Exchange> enderecos = new HashMap<>();

    public ExchangeResource(){}

    @GET
    public Exchange getEnderecoExchange(@QueryParam("empresa") String empresa) {

        for(Exchange e: enderecos.values()){
            if(e.empresas.contains(empresa))
                return e;
        }

        //Quer dizer que não encontrou a empresa!
        return null;
    }

    @GET
    @Path("/todas")
    public ArrayList<Exchange> getTodosEnderecos(){
        ArrayList<Exchange> res = new ArrayList<>();
        for(Exchange e: enderecos.values())
            res.add(e);
        return res;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(Exchange exchange) {
        System.out.println("Nova exchange");
        enderecos.put(exchange.endereco, exchange);
        return Response.ok().build();
    }

}
