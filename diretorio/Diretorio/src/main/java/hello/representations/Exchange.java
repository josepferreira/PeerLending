package hello.representations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class Exchange {
    public String endereco;
    public ArrayList<String> empresas;

    @JsonCreator
    public Exchange(@JsonProperty("endereco") String endereco, @JsonProperty("empresas") ArrayList<String> empresas) {
        this.endereco = endereco;
        this.empresas = empresas;
    }

}
