-module(chatv2).
-export([server/1]).


server(Port) ->
  login_manager:start(),
  {ok, LSock} = gen_tcp:listen(Port, [binary, {packet, line}, {reuseaddr, true}]),
  acceptor(LSock).

acceptor(LSock) ->
  {ok, Sock} = gen_tcp:accept(LSock),
  spawn(fun() -> acceptor(LSock) end),
  autenticaCliente(Sock).


  
autenticaCliente(Sock) ->
  
   receive
    {tcp,_,Message} -> io:format(ccs:decode_msg(Message,'Autenticacao'));

    %          Res = login_manager:login(User, Password),
     %         case Res of
      %          ok -> %entra no chat    
       %         invalid ->
        %          gen_tcp:send(Sock, "Autenticação inválida\n"),
         %         userNaoAutenticado(Sock)
          %    end;
    {tcp_closed,_} ->
          io:format("utilizador nao se autenticou nem registou e saiu~n",[]),
          true
  end.

user(Sock, User) ->
    io:format("User: " ++ User ++ " autenticado com sucesso!").