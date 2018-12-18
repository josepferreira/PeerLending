-module(frontend_state).
-export([start/0]).

-include("ccs.hrl").

%Neste modulo vao ser implementadas funcoes de comunicacao com exchange e gestao da logica dessa comunicacao
% MapEstado :  empresa -> {Socket da Exchange, Leilao, Emissao, UltimaTaxa, UtilizadoresInteressados = []}
% MapUser : username -> papel



%Falta ligar aos sockets da exchange e atualizar ultima taxa, se for para manter isso aqui


start() ->
  io:format("State ja esta a correr!"),
  %Ligacao aos sockets da exchange
  loop ( #{"emp1" => "empresa", "emp2" => "empresa", "cli1" => "cliente", "cli2" => "cliente"}, #{})
.

loop (MapUser, MapEstado) -> 
    receive
        {getPapel, User, From} ->
            io:format("Loop recebeu getPapel"),
            io:format("User : ~p", [User]),
            case maps:find(User, MapUser) of
                {ok, Papel} ->
                    io:format("Encontrou"),
                    From ! {frontend_state, Papel},
                    loop(MapUser, MapEstado);
                _ -> 
                    io:format("Não Encontrou"),
                    From ! {frontend_state, invalid},
                    loop(MapUser, MapEstado)
                
            end
        ;
        {licitacao, Empresa, User, From, ProtoBufBin}->
            io:format("Loop recebeu licitacao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {SockExch, Leilao, Emissao, UltimaTaxa, UtilizadoresInteressados }} when Leilao == true -> 
                    NovaLista = [{User, From} | UtilizadoresInteressados],
                    NewMap = maps:put(Empresa, {SockExch, Leilao, Emissao, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(MapUser, NewMap)
                ;
                _-> 
                    From ! invalid,
                    loop(MapUser, MapEstado)
            end
        ;
        {emissao, Empresa, User, From, ProtoBufBin}->
            io:format("Loop recebeu emissao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {SockExch, Leilao, Emissao, UltimaTaxa, UtilizadoresInteressados }} when Emissao == true -> 
                    NovaLista = [{User, From} | UtilizadoresInteressados],
                    NewMap = maps:put(Empresa, {SockExch, Leilao, Emissao, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(MapUser, NewMap)
                ;
                _-> 
                    From ! invalid,
                    loop(MapUser, MapEstado)
            end
        ;
        {iniciaLeilao, Empresa, From, ProtoBufBin}->
            io:format("Loop recebeu iniciaLeilao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {SockExch, Leilao, Emissao, UltimaTaxa, _}} when Leilao == false -> 
                    NovaLista = [],
                    NewMap = maps:put(Empresa, {SockExch, true, Emissao, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(MapUser, NewMap)
                ;
                _-> 
                    From ! invalid,
                    loop(MapUser, MapEstado)
            end
        ;
        {iniciaEmissao, Empresa, From, ProtoBufBin}->
            io:format("Loop recebeu iniciaEmissao ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {SockExch, Leilao, Emissao, UltimaTaxa, _}} when Emissao == false -> 
                    NovaLista = [],
                    NewMap = maps:put(Empresa, {SockExch, Leilao, true, UltimaTaxa, NovaLista}, MapEstado),
                    gen_tcp:send(SockExch, ProtoBufBin),
                    loop(MapUser, NewMap)
                ;
                _-> 
                    From ! invalid,
                    loop(MapUser, MapEstado)
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

