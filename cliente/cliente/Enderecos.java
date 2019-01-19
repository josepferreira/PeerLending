package cliente;

import java.io.*;
import java.util.*;
import java.net.*;

import org.json.*;

public class Enderecos{
    public String enderecoFrontEnd;
    public String portaFrontEnd;
    public String enderecoDiretorio;
    public String portaDiretorio;

    public void leiloesAtivos(){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/leilao");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void leiloesAtivos(String empresa){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/leilao?empresa=" + empresa);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
        
    }

    public void leiloesFinalizados(){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/leilao/finalizados");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void leiloesFinalizados(String empresa){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/leilao/finalizados?empresa=" + empresa);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void emissoesAtivas(){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/emissao");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void emissoesAtivas(String empresa){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/emissao?empresa=" + empresa);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void emissoesFinalizadas(){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/emissao/finalizadas");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }

    public void emissoesFinalizadas(String empresa){
        try{
            URL url = new URL("http://" + enderecoDiretorio + ":" + portaDiretorio + "/emissao/finalizadas?empresa=" + empresa);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
		    }
            in.close();
            
            String resposta = response.toString();
            JSONArray jsonArray = new JSONArray(resposta);
            System.out.println("Os leilões ativos são: ");
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject objetoJSON = jsonArray.getJSONObject(i);
                System.out.println("Empresa: " + objetoJSON.get("empresa"));
                System.out.println(objetoJSON.toString());
            }
            con.disconnect();

        }catch(Exception exc){
            System.out.println(exc);
        }
    }
}