-module(frontend_client).
-export([start/4]).

-include("ccs.hrl").

% Neste modulo vao ser implementadas funcoes de comunicação entre o cliente (java) e o frontend (erlang)
% Estes módulos não serão registados e cada "ator" tratara de um cliente
% Esta dividio em duas partes ... Um para tratar do cliente e outro para tratar da empresa

%Função que dá o PID do state correspondente à empresa
pidEmpresa(MapState, Empresa) ->
    Lista = maps:to_list(MapState),
    [{PidState, _}|_] = lists:filter ( fun( {_, ListEmpresas}) -> lists:member(Empresa, ListEmpresas) end, Lista),
    PidState. 

start(Sock, User, Papel, MapState) ->
    io:format("Foi aceite a conexao para um cliente: ~p~n", [Papel]),
    case Papel of
        "empresa" -> 
            io:format("Tou na empresa~n"),
            PidState = pidEmpresa(MapState, User), 
            loopEmpresa(Sock, User, PidState);
        "licitador" -> loopLicitador(Sock, User, MapState)
  end.

% vou criar 4 funções para comunicar com o exterior ... Criar leilao, criar emissao, licitar em leilao, licitar em emissao

%Se for para manter assim, para receber a mensagme do "state" posso por isso numa função a parte
loopEmpresa(Sock, User, PidState) ->
    io:format("Vou tratar da empresa~n"),
    receive
        %{tcp,_, _} -> io:format("Recebi uma mensagem~n");
        {tcp, _, MensagemEmpresa} ->
            io:format("Recebi uma mensagem do utilizador via TCP e agora vou tratar dela~n"),
            io:format(MensagemEmpresa),
            {'MensagemUtilizador', Tipo, _, Utilizador, _, _} = ccs:decode_msg(MensagemEmpresa, 'MensagemUtilizador'),
            %{'MensagemEmpresa', Tipo, _, _, Utilizador} = ccs:decode_msg(MensagemEmpresa,'MensagemEmpresa'),
            case Tipo of
                'LEILAO' -> 
                    %Vou ter de mandar a mensagem para o frontend_state
                    %Depois tenho de esperar a resposta dele
                    %No final reenviar para o cliente
                    %{iniciaLeilao, Empresa, From, ProtoBufBin}

                    PidState ! {iniciaLeilao, Utilizador, self(), MensagemEmpresa},
                    loopEmpresa(Sock, User, PidState)
                ;
                'EMISSAO' ->
                    %{iniciaEmissao, Empresa, From, ProtoBufBin}
                    PidState ! {iniciaEmissao, Utilizador, self(), MensagemEmpresa},
                    loopEmpresa(Sock, User, PidState)
                ;    
                _ -> 
                    io:format("Não recebemos uma Emissao nem Leilao, algo aqui correu mesmo muito mal"),
                    Binario = ccs:encode_msg(#'Resultado'{tipo='EMISSAO',empresa=Utilizador,texto="INSUCESSO"}),
                    gen_tcp:send(Sock, Binario),
                    %PidFront ! {self(), ok},
                    loopEmpresa(Sock, User, PidState)
            end
        ;
        {tcp_closed, _} ->
            Res = login_manager:logout(User),
            case Res of
                ok -> io:format("utilizador desautenticado~n",[]);
                _ -> io:format("algum erro de desautenticacao~n",[])
            end
        ;
        {tcp_error, _, _} ->
            Res = login_manager:logout(User),
            case Res of 
            ok -> io:format("utilizador desautenticado~n",[]);
            _ -> io:format("algum erro de desautenticacao~n",[])
            end
        ;
        %A receber a resposta do frontend_state
        {PidState, invalid, Erro} ->
            io:format(Erro),
            loopEmpresa(Sock, User, PidState)
        ;

        {PidState, Resposta} ->
            gen_tcp:send(Sock, Resposta),
            loopEmpresa(Sock, User, PidState)
    end.

loopLicitador(Sock, User, MapState) ->
    receive
        {tcp, _ , MensagemLicitador} ->
            io:format("Recebi uma mensagem do utilizador e agora vou tratar dela~n"),
            {'MensagemUtilizador', Tipo, _, Utilizador, _, Investidor} = ccs:decode_msg(MensagemLicitador, 'MensagemUtilizador'),
            %{'MensagemLicitador', Tipo, Leilao, Emissao, Utilizador} = ccs:decode_msg(MensagemLicitador,'MensagemLicitador'),
            case Tipo of
                'LEILAO' -> 
                    %Vou ter de mandar a mensagem para o frontend_state
                    %Depois tenho de esperar a resposta dele
                    %No final reenviar para o cliente
                    %{licitacao, Empresa, User, From, ProtoBufBin}
                    {'MensagemInvestidor', Leilao, _} = Investidor,
                    {'LicitacaoLeilao', Empresa, _, _} = Leilao,
                    PidState = pidEmpresa(MapState, User),
                    PidState ! {licitacao, Empresa, Utilizador, self(), MensagemLicitador},
                    loopLicitador(Sock, User, MapState)
                ;
                    % receive
                    %     {PidState, RespostaBinaria} ->
                    %         gen_tcp:send(Sock, RespostaBinaria),
                    %      %   PidFront ! {self(), ok},
                    %         loopLicitador(Sock, User, MapState);
                    %     {PidState, invalid} ->
                    %         io:format("A resposta foi invalida ... Necessário tratar da resposta ao cliente!"),
                    %         loopLicitador(Sock, User, MapState)
                    % end;
                'EMISSAO' ->
                    io:format('Emissao~n'),
                    %{emissao, Empresa, User, From, ProtoBufBin}
                    %{'SubscricaoTaxaFixa', Empresa, _} = Emissao,
                    {'MensagemInvestidor', _, Emissao} = Investidor,
                    {'SubscricaoTaxaFixa', Empresa, _} = Emissao,
                    io:format(Empresa),
                    io:format('~n'),
                    PidState = pidEmpresa(MapState, Empresa),
                    PidState ! {emissao, Empresa, Utilizador, self(), MensagemLicitador},
                    loopLicitador(Sock, User, MapState)
                    % receive
                    %     {PidState, RespostaBinaria} ->
                    %         gen_tcp:send(Sock, RespostaBinaria),
                    %         %PidFront ! {self(), ok},
                    %         loopLicitador(Sock, User, MapState);
                    %     {PidState, invalid} ->
                    %         io:format("A resposta foi invalida ... Necessário tratar da resposta ao cliente!"),
                    %         loopLicitador(Sock, User, MapState)
                    % end;
                ;
                _ -> 
                    io:format("Não recebemos uma Emissao nem Leilao, algo aqui correu mesmo muito mal"),
                    Binario = ccs:encode_msg(#'Resultado'{tipo='EMISSAO',empresa=Utilizador,texto="INSUCESSO"}),
                    gen_tcp:send(Sock, Binario),
                    %PidFront ! {self(), ok},
                    loopLicitador(Sock, User, MapState)
            end
        ;
        {tcp_closed, _} ->
            Res = login_manager:logout(User),
            case Res of
            ok -> io:format("utilizador desautenticado~n",[]);
            _ -> io:format("algum erro de desautenticacao~n",[])
            end
        ;
        {tcp_error, _, _} ->
            Res = login_manager:logout(User),
            case Res of 
            ok -> io:format("utilizador desautenticado~n",[]);
            _ -> io:format("algum erro de desautenticacao~n",[])
            end
        ;
        %A receber a resposta do frontend_state
        {Pid, Resposta} ->
            case maps:is_key(Pid, MapState) of
                true -> 
                    gen_tcp:send(Sock, Resposta),
                    loopLicitador(Sock, User, MapState)
                ;
                false ->
                    io:format("Erro na linha 156~n"),
                    loopLicitador(Sock, User, MapState)
            end
    end
.

%Só para ficar guardado o decode e encode das mensagens

%{'CriacaoLeilao', MontanteLeilao, Taxa} = Leilao,
%                    io:format("~p~n", [MontanteLeilao]),
%                   io:format("~f~n", [Taxa]),
%                    %Aqui vou ter de trocar mensagens com o frontend_state
%                    Binario = ccs:encode_msg(#'Resultado'{tipo='LEILAO',empresa=Utilizador,texto="Leilao criado com sucesso"}),
%                    gen_tcp:send(Sock, Binario),
%                    PidFront ! {self(), ok},
%                    loopEmpresa(Sock);

%{'EmissaoTaxaFixa', MontanteEmissao} = Emissao,
%                    io:format("~p~n", [MontanteEmissao]),
%                    %Aqui vou ter de trocar mensagens com o frontend_state
%                    Binario = ccs:encode_msg(#'Resultado'{tipo='EMISSAO',empresa=Utilizador,texto="Emissao criada com sucesso"}),
%                    gen_tcp:send(Sock, Binario),
%                    PidFront ! {self(), ok},
%                    loopEmpresa(Sock);

% Enviar mensagens com duas estruturas 

% Binario = ccs:encode_msg(#'RespostaExchange'{tipo='RESULTADO',resultado=#'Resultado'{tipo='LEILAO',empresa="emp1",texto="Nao foste tu"}}),
%                     gen_tcp:send(Sock, Binario);