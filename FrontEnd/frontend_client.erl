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

loopEmpresa(Sock) ->
    receive
        {PidFront, MensagemEmpresa} ->
            io:format("Recebi uma mensagem do utilizador e agora vou tratar dela~n"),
            {'MensagemEmpresa', Tipo, Leilao, Emissao, Utilizador} = ccs:decode_msg(MensagemEmpresa,'MensagemEmpresa'),
            case Tipo of
                'LEILAO' -> 
                    {'CriacaoLeilao', MontanteLeilao, Taxa} = Leilao,
                    io:format("~p~n", [MontanteLeilao]),
                    io:format("~f~n", [Taxa]),
                    %Aqui vou ter de trocar mensagens com o frontend_state
                    Binario = ccs:encode_msg(#'Resultado'{tipo='LEILAO',empresa=Utilizador,texto="Leilao criado com sucesso"}),
                    gen_tcp:send(Sock, Binario),
                    PidFront ! {self(), ok},
                    loopEmpresa(Sock);
                'EMISSAO' ->
                    {'EmissaoTaxaFixa', MontanteEmissao} = Emissao,
                    io:format("~p~n", [MontanteEmissao]),
                    %Aqui vou ter de trocar mensagens com o frontend_state
                    Binario = ccs:encode_msg(#'Resultado'{tipo='EMISSAO',empresa=Utilizador,texto="Emissao criada com sucesso"}),
                    gen_tcp:send(Sock, Binario),
                    PidFront ! {self(), ok},
                    loopEmpresa(Sock);
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
        _ -> io:format("LA LA LA")
    end
.

