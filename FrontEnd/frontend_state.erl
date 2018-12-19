-module(frontend_state).
-export([start/1]).

-include("ccs.hrl").

%Neste modulo vao ser implementadas funcoes de comunicacao com exchange e gestao da logica dessa comunicacao
% MapEstado :  empresa -> {Socket da Exchange, Leilao, Emissao, UltimaTaxa, UtilizadoresInteressados = []}



%Falta ligar aos sockets da exchange e atualizar ultima taxa, se for para manter isso aqui


start(SockExch) ->
  io:format("State ja esta a correr!"),
  %Ligacao aos sockets da exchange
  MyPid = spawn (fun() -> loop (SockExch, #{}) end),
  MyPid
.

loop (SockExch, MapEstado) -> 
    receive
        %Receive para as mensagens vindas do frontend_client
        {licitacao, Empresa, User, From, ProtoBufBin}->
            io:format("Loop recebeu licitacao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, UltimaTaxa, UtilizadoresInteressados }} when Leilao == true -> 
                    NovaLista = [{User, From} | UtilizadoresInteressados],
                    NewMap = maps:put(Empresa, {Leilao, Emissao, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(SockExch, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, "Não ha um leilao em curso para a empresa" ++ Empresa},
                    loop(SockExch, MapEstado)
            end
        ;
        {emissao, Empresa, User, From, ProtoBufBin}->
            io:format("Loop recebeu emissao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, UltimaTaxa, UtilizadoresInteressados }} when Emissao == true -> 
                    NovaLista = [{User, From} | UtilizadoresInteressados],
                    NewMap = maps:put(Empresa, {Leilao, Emissao, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(SockExch, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, "Não ha uma emissao em curso para a empresa" ++ Empresa},
                    loop(SockExch, MapEstado)
            end
        ;
        {iniciaLeilao, Empresa, From, ProtoBufBin}->
            io:format("Loop recebeu iniciaLeilao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, UltimaTaxa, _}} when Leilao == false , Emissao == false -> 
                    NovaLista = [],
                    NewMap = maps:put(Empresa, {true, Emissao, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(SockExch, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, "Não pode criar um leilao de momento! Já se encontra em atividade!"},
                    loop(SockExch, MapEstado)
            end
        ;
        {iniciaEmissao, Empresa, From, ProtoBufBin}->
            io:format("Loop recebeu iniciaEmissao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, UltimaTaxa, _}} when Emissao == false , Leilao == false -> 
                    NovaLista = [],
                    NewMap = maps:put(Empresa, {Leilao, true, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(SockExch, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, "Não pode criar uma emissao de momento! Já se encontra em atividade!"},
                    loop(SockExch, MapEstado)
            end
        ;
        %Receive para as mensagens das exchanges
        {tcp, _, RespostaExchange} ->
            {'RespostaExchange', Tipo, Notificacao, Resultado} = ccs:decode_msg(RespostaExchange,'RespostaExchange'),
            case Tipo of
                'RESULTADO' -> 
                    {_, Empresa, _} = Resultado,
                    case maps:find(Empresa, MapEstado) of 
                        {ok, {_, _, UltimaTaxa, ListaUsers}} ->
                            [UserPid ! {self(), RespostaExchange} || {_, UserPid} <- ListaUsers ],
                            NewMap = maps:put(Empresa, {false, false, UltimaTaxa, []}),
                            loop(SockExch, NewMap)
                    end
                ;
                'NOTIFICACAO' ->
                    {_, Empresa, Utilizador, _, _, _} = Notificacao,
                    case maps:find(Empresa, MapEstado) of 
                        {ok, {_, _, _, ListaUsers}} ->
                            [{_, UserPid }] = lists:filter( fun({U,_}) -> U == Utilizador end, ListaUsers),
                            UserPid ! {self(), RespostaExchange}
                        ;
                        _ ->
                            io:format("Erro na linha 101")
                    end
            end
    end
.



% rpc( Req ) ->
%     frontend_state ! Req,
%     receive
%         {frontend_state, Res } -> 
%             %io:format("RPC recebeu ~p~n", [Res]),
%             Res
%     end
% .

% getPapel (User) ->
%     io:format("Dentro do getPapel do frontend_state"),
%     rpc ({getPapel, User, self()}) 
    
% .


% licitacao (Valor, Empresa, User) ->
%     io:format("Dentro do licitacao do frontend_state"),
%     rpc ({licitacao, Valor, Empresa, User, self()}) 
    
% .

