-module(frontend).
-export([start/0]).

-include("ccs.hrl").


start() ->
  login_manager:start(),
  frontend_state:start(),
  io:format("Servidor principal ja esta a correr!"),
  {ok, LSock} = gen_tcp:listen(12345, [binary, {packet, 4}, {active, true}, {reuseaddr, true}]),
  acceptor(LSock).

acceptor(LSock) ->
  {ok, Sock} = gen_tcp:accept(LSock),
  spawn(fun() -> acceptor(LSock) end),
  autenticaCliente(Sock),
  acceptor(LSock).


  
autenticaCliente(Sock) ->
   io:format("Vou receber\n"),
   receive
      {tcp, _, Autenticacao} -> 

              io:format("cheguei~n",[]),
              {'Autenticacao', User, Pass} = ccs:decode_msg(Autenticacao,'Autenticacao'),
              io:format("User: "),
              io:format(User),
              io:format("!\nPass: "),
              io:format(Pass),
              io:format("!\n"),
              Resposta = login_manager:login(User,Pass),
              io:format(Resposta),
              io:format("\n"),
              case Resposta of
                ok -> 
                    io:format("Vou pedir papel~n"),
                    Papel = frontend_state:getPapel(User),
                    io:format("Já recebi papel do State e papel : ~p~n",[Papel]),
                    Bin = ccs:encode_msg(#'RespostaAutenticacao'{sucesso = true, papel = Papel}),
                    gen_tcp:send(Sock, Bin),
                    %%Agora vou iniciar o ator que vai tratar do cliente
                    Pid = spawn(frontend_client, start, [Sock, Papel]),
                    io:format("O PID do processo criado é: ~p~n", [Pid]),
                    user(Sock, User, Pid);

                invalid -> autenticaCliente(Sock)
              end
            ;
          %          gen_tcp:send(Sock, "Autenticação inválida\n"),
      {tcp_closed,_} ->
            io:format("utilizador nao se autenticou nem registou e saiu~n",[]),
            true
    end.


%Este ator vai receber a mensaggem e reenchaminhar para o respetivo ator ... Eu nao consegui com que fosse o outro a esperar pela mensagem ...
user(Sock, User, Pid) ->
    
    receive
      {tcp,_,Msg} -> io:format("User: " ++ User ++ " autenticado com sucesso!\n"),
                   Pid ! {self(), Msg},
                   receive
                     {Pid, ok} -> io:format("Recebi um okay~n")
                   end,
                   user(Sock, User, Pid);
      {tcp_closed, _} ->
        Res = login_manager:logout(User),
        case Res of
          ok -> io:format("utilizador desautenticado~n",[]);
          _ -> io:format("algum erro de desautenticacao~n",[])
        end;
      {tcp_error, _, _} ->
        Res = login_manager:logout(User),
        case Res of 
          ok -> io:format("utilizador desautenticado~n",[]);
          _ -> io:format("algum erro de desautenticacao~n",[])
        end
    end.