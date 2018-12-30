import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
//import java.util.Iterator;
//import java.util.ArrayList;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/**
 * @author Crunchify.com
 */
class Pagina implements Comparable{
    private String titulo;
    private String id;
    private JSONObject jo;

    public Pagina(String titulo, String id){
        this.titulo = titulo;
        this.id = id;
        jo = new JSONObject();
        jo.put("id",id);
        jo.put("titulo", titulo);
    }

    public String getTitulo(){
        return this.titulo;
    }

    public String getId(){
        return this.id;
    }

    public JSONObject getJo(){
        return this.jo;
    }

    public int compareTo(Object p) {
        String aux = ((Pagina) p).getTitulo();
        /* For Ascending order*/
        return this.titulo.compareTo(aux);

    }
}

public class AnalisaJSON {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        
        ArrayList<Pagina> paginas = new ArrayList<>();

        for(String s: args) {
            System.out.println("Vou ler o ficheiro " + s);
            Object obj = new JSONParser().parse(new FileReader(s));

            JSONObject jsonObject = (JSONObject) obj;

            String id = (String) jsonObject.get("_id");
            String titulo = (String) jsonObject.get("titulo");

            paginas.add(new Pagina(titulo, id));

            System.out.println("Name: " + id);
            System.out.println("Author: " + titulo);
        }

        Collections.sort(paginas);

        for(Pagina p: paginas)
        System.out.println(p.getTitulo());

        JSONArray ja = new JSONArray();

        for(Pagina p: paginas)
            ja.add(p.getJo());
        
        PrintWriter pw = new PrintWriter("index.json");
        pw.write(ja.toJSONString());

        pw.flush();
        pw.close();
    }
}