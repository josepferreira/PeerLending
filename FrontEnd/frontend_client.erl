-module(frontend_client).
-export([start/2]).

-include("ccs.hrl").

% Neste modulo vao ser implementadas funcoes de comunicação entre o cliente (java) e o frontend (erlang)
% Estes módulos não serão registados e cada "ator" tratara de um cliente
% Esta dividio em duas partes ... Um para tratar do cliente e outro para tratar da empresa

start(Sock, Papel) ->
  io:format("Foi aceite a conexao para um cliente: ~p~n", [Papel]),
  case Papel of
      "empresa" -> io:format("Tou na empresa~n"), loopEmpresa(Sock);
      "cliente" -> loopLicitador(Sock)
  end.

% vou criar 4 funções para comunicar com o exterior ... Criar leilao, criar emissao, licitar em leilao, licitar em emissao

%Se for para manter assim, para receber a mensagme do "state" posso por isso numa função a parte
loopEmpresa(Sock) ->
    receive
        {PidFront, MensagemEmpresa} ->
            io:format("Recebi uma mensagem do utilizador e agora vou tratar dela~n"),
            {'MensagemEmpresa', Tipo, _, _, Utilizador} = ccs:decode_msg(MensagemEmpresa,'MensagemEmpresa'),
            case Tipo of
                'LEILAO' -> 
                    %Vou ter de mandar a mensagem para o frontend_state
                    %Depois tenho de esperar a resposta dele
                    %No final reenviar para o cliente
                    %{iniciaLeilao, Empresa, From, ProtoBufBin}
                    frontend_state ! {iniciaLeilao, Utilizador, self(), MensagemEmpresa},
                    receive
                        {frontend_state, RespostaBinaria} ->
                            gen_tcp:send(Sock, RespostaBinaria),
                            PidFront ! {self(), ok},
                            loopEmpresa(Sock);
                        {frontend_state, invalid} ->
                            io:format("A resposta foi invalida ... Necessário tratar da resposta ao cliente!"),
                            loopEmpresa(Sock)
                    end;
                'EMISSAO' ->
                    %{iniciaEmissao, Empresa, From, ProtoBufBin}
                    frontend_state ! {iniciaEmissao, Utilizador, self(), MensagemEmpresa},
                    receive
                        {frontend_state, RespostaBinaria} ->
                            gen_tcp:send(Sock, RespostaBinaria),
                            PidFront ! {self(), ok},
                            loopEmpresa(Sock);
                        {frontend_state, invalid} ->
                            io:format("A resposta foi invalida ... Necessário tratar da resposta ao cliente!"),
                            loopEmpresa(Sock)
                    end;
                _ -> 
                    io:format("Não recebemos uma Emissao nem Leilao, algo aqui correu mesmo muito mal"),
                    Binario = ccs:encode_msg(#'Resultado'{tipo='EMISSAO',empresa=Utilizador,texto="INSUCESSO"}),
                    gen_tcp:send(Sock, Binario),
                    PidFront ! {self(), ok},
                    loopEmpresa(Sock)
            end
    end.

loopLicitador(Sock) ->
    receive
        {PidFront, MensagemLicitador} ->
            io:format("Recebi uma mensagem do utilizador e agora vou tratar dela~n"),
            {'MensagemLicitador', Tipo, Leilao, Emissao, Utilizador} = ccs:decode_msg(MensagemLicitador,'MensagemLicitador'),
            case Tipo of
                'LEILAO' -> 
                    %Vou ter de mandar a mensagem para o frontend_state
                    %Depois tenho de esperar a resposta dele
                    %No final reenviar para o cliente
                    %{licitacao, Empresa, User, From, ProtoBufBin}
                    {'LicitacaoLeilao', Empresa, _, _} = Leilao,
                    frontend_state ! {licitacao, Empresa, Utilizador, self(), MensagemLicitador},
                    receive
                        {frontend_state, RespostaBinaria} ->
                            gen_tcp:send(Sock, RespostaBinaria),
                            PidFront ! {self(), ok},
                            loopLicitador(Sock);
                        {frontend_state, invalid} ->
                            io:format("A resposta foi invalida ... Necessário tratar da resposta ao cliente!"),
                            loopLicitador(Sock)
                    end;
                'EMISSAO' ->
                    %{emissao, Empresa, User, From, ProtoBufBin}
                    {'SubscricaoTaxaFixa', Empresa, _} = Emissao,
                    frontend_state ! {emissao, Empresa, Utilizador, self(), MensagemLicitador},
                    receive
                        {frontend_state, RespostaBinaria} ->
                            gen_tcp:send(Sock, RespostaBinaria),
                            PidFront ! {self(), ok},
                            loopLicitador(Sock);
                        {frontend_state, invalid} ->
                            io:format("A resposta foi invalida ... Necessário tratar da resposta ao cliente!"),
                            loopLicitador(Sock)
                    end;
                _ -> 
                    io:format("Não recebemos uma Emissao nem Leilao, algo aqui correu mesmo muito mal"),
                    Binario = ccs:encode_msg(#'Resultado'{tipo='EMISSAO',empresa=Utilizador,texto="INSUCESSO"}),
                    gen_tcp:send(Sock, Binario),
                    PidFront ! {self(), ok},
                    loopLicitador(Sock)
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