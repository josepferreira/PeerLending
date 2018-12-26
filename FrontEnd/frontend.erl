-module(frontend).
-export([start/0]).

-include("ccs.hrl").


%Função para criar um mapa: PidState - [Empresa]
mapaStateEmpresa(Map, Push, Pull) ->
  PidStateA = frontend_state:start(Push, Pull),
  Map2 = maps:put(PidStateA, ["emp1", "emp2"], Map),
  Map2
.

start() ->
  login_manager:start(),
  {ok, Socket} = chumak:socket(push),

  case chumak:connect(Socket, tcp, "localhost", 12351) of
      {ok, _} ->
          io:format("Binding OK with Pid: ~p\n", [Socket]);
      {error, Reason} ->
          io:format("Connection Failed for this reason: ~p\n", [Reason]);
      X ->
          io:format("Unhandled reply for bind ~p \n", [X])
  end,
  
  SocketExchPush = application:start(chumak),

  {ok, Socket1} = chumak:socket(pull),

  case chumak:connect(Socket1, tcp, "localhost", 12350) of
      {ok, _BindPid} ->
          io:format("Binding OK with Pid: ~p\n", [Socket1]);
      {error, Reason1} ->
          io:format("Connection Failed for this reason: ~p\n", [Reason1]);
      X1 ->
          io:format("Unhandled reply for bind ~p \n", [X1])
  end,
  SocketExchPull = application:start(chumak),

    
  MapState = mapaStateEmpresa(#{}, SocketExchPush, SocketExchPull),

  io:format("Servidor principal ja esta a correr!"),
  {ok, LSock} = gen_tcp:listen(12345, [binary, {packet, 4}, {active, true}, {reuseaddr, true}]),
  acceptor(LSock, MapState).

acceptor(LSock, MapState) ->
  {ok, Sock} = gen_tcp:accept(LSock),
  spawn(fun() -> acceptor(LSock, MapState) end),
  autenticaCliente(Sock, MapState),
  acceptor(LSock, MapState).


  
autenticaCliente(Sock, MapState) ->
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
                "licitador" -> 
                    io:format("Vou pedir papel~n"),
                    Papel = Resposta,
                    io:format("Já recebi papel do State e papel : ~p~n",[Papel]),
                    Bin = ccs:encode_msg(#'RespostaAutenticacao'{sucesso = true, papel = Papel}),
                    gen_tcp:send(Sock, Bin),
                    %%Agora vou iniciar o ator que vai tratar do cliente
                    Pid = spawn(frontend_client, start, [Sock, User, Papel, MapState]),
                    gen_tcp:controlling_process(Sock, Pid),
                    io:format("O PID do processo criado é: ~p~n", [Pid]);

                "empresa" -> 
                    io:format("Vou pedir papel~n"),
                    Papel = Resposta,
                    io:format("Já recebi papel do State e papel : ~p~n",[Papel]),
                    Bin = ccs:encode_msg(#'RespostaAutenticacao'{sucesso = true, papel = Papel}),
                    gen_tcp:send(Sock, Bin),
                    %%Agora vou iniciar o ator que vai tratar do cliente
                    Pid = spawn(frontend_client, start, [Sock, User, Papel, MapState]),
                    gen_tcp:controlling_process(Sock, Pid),
                    io:format("O PID do processo criado é: ~p~n", [Pid])
                  ;
                    %user(Sock, User, Pid);


                invalid -> autenticaCliente(Sock, MapState)
              end
            ;
          %          gen_tcp:send(Sock, "Autenticação inválida\n"),
      {tcp_closed,_} ->
            io:format("utilizador nao se autenticou nem registou e saiu~n",[]),
            true
    end.


% %Este ator vai receber a mensaggem e reenchaminhar para o respetivo ator ... Eu nao consegui com que fosse o outro a esperar pela mensagem ...
% user(Sock, User, Pid) ->
    
%     receive
%       {tcp,_,Msg} -> io:format("User: " ++ User ++ " autenticado com sucesso!\n"),
%                    Pid ! {self(), Msg},
%                    receive
%                      {Pid, ok} -> io:format("Recebi um okay~n")
%                    end,
%                    user(Sock, User, Pid);
%       {tcp_closed, _} ->
%         Res = login_manager:logout(User),
%         case Res of
%           ok -> io:format("utilizador desautenticado~n",[]);
%           _ -> io:format("algum erro de desautenticacao~n",[])
%         end;
%       {tcp_error, _, _} ->
%         Res = login_manager:logout(User),
%         case Res of 
%           ok -> io:format("utilizador desautenticado~n",[]);
%           _ -> io:format("algum erro de desautenticacao~n",[])
%         end
%     end.