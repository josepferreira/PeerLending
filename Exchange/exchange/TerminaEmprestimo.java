package exchange;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.FileReader;
import java.util.Iterator;
import org.json.*;



import java.io.*;
import java.util.*;
import java.net.*;

import org.zeromq.ZMQ;

import exchange.Ccs.*;

class TerminaEmprestimo implements Runnable{
    LocalDateTime fim;
    ZMQ.Context context;
    public ZMQ.Socket terminar;

    public TerminaEmprestimo(LocalDateTime f, ZMQ.Context c){
        
        context = c;
        terminar = context.socket(ZMQ.PUSH);
        terminar.connect("inproc://terminar");
        fim = f;
    }

    public void run(){
        long dormir = LocalDateTime.now().until(fim,ChronoUnit.MILLIS);
        System.out.println("Vou dorimir: " + dormir);
        if(dormir > 0){
            try{
                Thread.sleep(dormir);
            }
            catch(Exception exc){
                System.out.println(exc.getMessage());
            }
        }
        //enviar mensagem
        String mensagem = "::terminar::";
        terminar.send(mensagem.getBytes());
    }
}
