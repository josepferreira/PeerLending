-module(frontend).
-export([start/0]).

-include("ccs.hrl").


start() ->
  login_manager:start(),
  io:format("Servidor ja esta a correr!"),
  {ok, LSock} = gen_tcp:listen(12345, [binary, {packet, 4}, {active, true}, {reuseaddr, true}]),
  acceptor(LSock).

acceptor(LSock) ->
  {ok, Sock} = gen_tcp:accept(LSock),
  spawn(fun() -> acceptor(LSock) end),
  autenticaCliente(Sock).


  
autenticaCliente(Sock) ->
   io:format("Vou receber\n"),
   receive
      {tcp,_,Autenticacao} -> io:format("cheguei~n",[]),
              {'Autenticacao',User,Pass} = ccs:decode_msg(Autenticacao,'Autenticacao'),
              io:format("User: "),
              io:format(User),
              io:format("!\nPass: "),
              io:format(Pass),
              io:format("!\n"),
              Resposta = login_manager:login(User,Pass),
              io:format(Resposta),
              io:format("\n"),
              case Resposta of
                ok -> Bin = ccs:encode_msg(#'RespostaAutenticacao'{sucesso = true, papel = "empresa"}),
                    gen_tcp:send(Sock, Bin),
                    user(Sock, User);
                invalid -> autenticaCliente(Sock)
              end;
          %          gen_tcp:send(Sock, "Autenticação inválida\n"),
      {tcp_closed,_} ->
            io:format("utilizador nao se autenticou nem registou e saiu~n",[]),
            true
    end.

user(Sock, User) ->
    
    receive
      {tcp,_,Data} -> io:format("User: " ++ User ++ " autenticado com sucesso!\n"),
                    user(Sock, User);
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