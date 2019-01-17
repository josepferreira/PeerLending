-module(frontend_state).
-export([start/3]).

-include("ccs.hrl").

%Neste modulo vao ser implementadas funcoes de comunicacao com exchange e gestao da logica dessa comunicacao
% MapEstado :  empresa -> {Leilao, Emissao, UtilizadoresInteressados = []}

recebeExchange(Pull, Loop) ->
    io:format("recebeExchange a correr~n"),
    {ok, Data} = chumak:recv(Pull),
    Loop ! {exchangeReceiver, Data},
    io:format("recebeExchange recebeu mensagem da exchange e enviou ao Loop~n"),
    recebeExchange(Pull, Loop)
.


geraMapa(M, []) ->
    M
;
geraMapa(M , [H | T]) ->
    M1 = maps:put(H, {false, false, []}, M),
    geraMapa(M1,T)
.

start(Push, Pull, ListaEmpresas) ->
  io:format("State ja esta a correr!~n"),

  Map = geraMapa(#{}, ListaEmpresas),
  %#{"emp1" => {false, false, []}, "emp2" => {false, false, []}}

  % Colocar loop a correr, fica a receber mensagens vindas do frontend e do exchangeReceiver
  MyPid = spawn (fun() -> loop (Push, Pull, Map) end),
  
  % Coloca o recebeExchange a correr, fica bloqueado a receber mensagens da exchange e envia-as para o loop
  _ = spawn( fun() -> recebeExchange(Pull, MyPid) end),
  MyPid
.

loop (Push, Pull, MapEstado) -> 
    receive
        %Receive para as mensagens vindas do frontend_client
        {licitacao, Empresa, User, From, ProtoBufBin}->
            io:format("Loop recebeu licitacao vinda do frontend ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, UtilizadoresInteressados }} when Leilao == true -> 
                    NovaLista = [{User, From} | UtilizadoresInteressados],
                    NewMap = maps:put(Empresa, {Leilao, Emissao, NovaLista}, MapEstado),
                    chumak:send(Push, ProtoBufBin),
                    loop(Push, Pull, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, leilao, "Não ha um leilao em curso para a empresa" ++ Empresa},
                    loop(Push, Pull, MapEstado)
            end
        ;
        {emissao, Empresa, User, From, ProtoBufBin}->
            io:format("Loop recebeu emissao vinda do frontend ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, UtilizadoresInteressados }} when Emissao == true -> 
                    NovaLista = [{User, From} | UtilizadoresInteressados],
                    NewMap = maps:put(Empresa, {Leilao, Emissao, NovaLista}, MapEstado),
                    chumak:send(Push, ProtoBufBin),
                    loop(Push, Pull, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, emissao, "Não ha uma emissao em curso para a empresa" ++ Empresa},
                    loop(Push, Pull, MapEstado)
            end
        ;
        {iniciaLeilao, Empresa, From, ProtoBufBin}->
            io:format("Loop recebeu iniciaLeilao vinda do frontend ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, _}} when Leilao == false , Emissao == false -> 
                    NovaLista = [{Empresa, From}],
                    NewMap = maps:put(Empresa, {false, Emissao, NovaLista}, MapEstado),
                    chumak:send(Push, ProtoBufBin),
                    % Binario = ccs:encode_msg(#'RespostaExchange'{tipo='RESULTADO',resultado=#'Resultado'{tipo='LEILAO',empresa="emp1",texto="Nao foste tu"}}),
                    % From ! {self(), Binario},
                    loop(Push, Pull, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, leilao, "Não pode criar um leilao de momento! Já se encontra em atividade!"},
                    loop(Push, Pull, MapEstado)
            end
        ;
        {iniciaEmissao, Empresa, From, ProtoBufBin}->
            io:format("Loop recebeu iniciaEmissao vinda do frontend ~n"),
            case maps:find(Empresa, MapEstado) of
                {ok, {Leilao, Emissao, _}} when Emissao == false , Leilao == false -> 
                    NovaLista = [{Empresa, From}],
                    NewMap = maps:put(Empresa, {Leilao, false, NovaLista}, MapEstado),
                    chumak:send(Push, ProtoBufBin),
                    loop(Push, Pull, NewMap)
                ;
                _-> 
                    From ! {self(), invalid, emissao, "Não pode criar uma emissao de momento! Já se encontra em atividade!"},
                    loop(Push, Pull, MapEstado)
            end
        ;
        %Receive para as mensagens das exchanges
        {exchangeReceiver, RespostaExchange} ->

            io:format("Loop recebeu Mensagem da exchange vinda do exchangeReceiver ~n"),
            
            {'RespostaExchange', Tipo, Notificacao, Resultado, Resposta} = ccs:decode_msg(RespostaExchange,'RespostaExchange'),
            case Tipo of
                'RESULTADO' ->
                    io:format("É do tipo RESULTADO ~n"),
                    {_, _,Empresa, _} = Resultado,
                    case maps:find(Empresa, MapEstado) of 
                        {ok, {_, _,  ListaUsers}} ->
                            [UserPid ! {self(), RespostaExchange} || {_, UserPid} <- ListaUsers ],
                            NewMap = maps:put(Empresa, {false, false,  []}, MapEstado),
                            loop(Push, Pull, NewMap)
                        ;
                        _ ->
                            loop(Push, Pull, MapEstado)
                    end
                ;
                'NOTIFICACAO' ->
                    io:format("É do tipo NOTIFICACAO ~n"),
                    {_, Empresa, Utilizador, _, _, _} = Notificacao,
                    case maps:find(Empresa, MapEstado) of 
                        {ok, {_, _, _, ListaUsers}} ->
                            [{_, UserPid }] = lists:filter( fun({U,_}) -> U == Utilizador end, ListaUsers),
                            UserPid ! {self(), RespostaExchange},
                            loop(Push, Pull, MapEstado)
                        ;
                        _ ->
                            io:format("Erro na linha 120"),
                            loop(Push, Pull, MapEstado)
                    end
                ;
                'RESPOSTA' ->
                    io:format("~p~n",[Resposta]),
                    {_, TipoOperacao, Utilizador, Sucesso, _} = Resposta,
                    case Sucesso of
                        false ->
                            io:format("Teve sucesso a ação, quer seja empresa ou licitador!"),
                            enviaMensagem(MapEstado, Utilizador, RespostaExchange),
                            loop(Push, Pull, MapEstado)
                        ;
                        true ->
                            case maps:find(Utilizador, MapEstado) of
                                {ok, {Leilao, Emissao, Lista}} ->
                                    case TipoOperacao of 
                                        'LEILAO' ->
                                            NewMap = maps:put(Utilizador, {true, Emissao, Lista}, MapEstado),
                                            enviaMensagem(NewMap, Utilizador, RespostaExchange),
                                            loop(Push, Pull, NewMap)
                                        ;
                                        'EMISSAO' ->
                                            NewMap = maps:put(Utilizador, {Leilao, true, Lista}, MapEstado),
                                            enviaMensagem(NewMap, Utilizador, RespostaExchange),
                                            loop(Push, Pull, NewMap)
                                    end
                                ;
                                _ ->
                                    io:format("O utilizador nao é uma empresa!!!"), 
                                    enviaMensagem(MapEstado, Utilizador, RespostaExchange),
                                    loop(Push, Pull, MapEstado)
                            end
                    end
            end
    end
.

enviaMensagem(MapEstado, Utilizador, RespostaExchange) ->
    io:format("É do tipo RESPOSTA ~n"),
    io:format("É PRECISO ENCONTRAR O UTILIZADOR ~s~n", [Utilizador]),
    Values = maps:values(MapEstado),
    io:format("~p~n", [Values]),
    ListaListas = [Lista || {_,_,Lista} <- Values],
    Utilizadores = append2(ListaListas),
    io:format("Lista = ~p", [Utilizadores]),
    [{Utilizador, UserPid}|_] = lists:filter( fun( {U,_}) -> U == Utilizador end, Utilizadores),
    UserPid ! {self(), RespostaExchange}
.

append2(List) -> append2(List,[]).
append2([], Acc) -> Acc;
append2([H|T],Acc) -> append2(T, H ++ Acc).