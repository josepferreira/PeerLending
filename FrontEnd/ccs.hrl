%% -*- coding: utf-8 -*-
%% Automatically generated, do not edit
%% Generated by gpb_compile version 4.4.1

-ifndef(ccs).
-define(ccs, true).

-define(ccs_gpb_version, "4.4.1").

-ifndef('AUTENTICACAO_PB_H').
-define('AUTENTICACAO_PB_H', true).
-record('Autenticacao',
        {username               :: iolist(),        % = 1
         password               :: iolist()         % = 2
        }).
-endif.

-ifndef('RESPOSTAAUTENTICACAO_PB_H').
-define('RESPOSTAAUTENTICACAO_PB_H', true).
-record('RespostaAutenticacao',
        {sucesso                :: boolean() | 0 | 1, % = 1
         papel                  :: iolist() | undefined % = 2
        }).
-endif.

-ifndef('MENSAGEMEMPRESA_PB_H').
-define('MENSAGEMEMPRESA_PB_H', true).
-record('MensagemEmpresa',
        {tipo = 'LEILAO'        :: 'LEILAO' | 'EMISSAO' | integer(), % = 1, enum TipoMensagem
         leilao                 :: ccs:'CriacaoLeilao'() | undefined, % = 2
         emissao                :: ccs:'EmissaoTaxaFixa'() | undefined, % = 3
         utilizador             :: iolist()         % = 4
        }).
-endif.

-ifndef('CRIACAOLEILAO_PB_H').
-define('CRIACAOLEILAO_PB_H', true).
-record('CriacaoLeilao',
        {montante               :: integer(),       % = 1, 32 bits
         taxa                   :: float() | integer() | infinity | '-infinity' | nan % = 2
        }).
-endif.

-ifndef('EMISSAOTAXAFIXA_PB_H').
-define('EMISSAOTAXAFIXA_PB_H', true).
-record('EmissaoTaxaFixa',
        {montante               :: integer()        % = 1, 32 bits
        }).
-endif.

-ifndef('MENSAGEMINVESTIDOR_PB_H').
-define('MENSAGEMINVESTIDOR_PB_H', true).
-record('MensagemInvestidor',
        {tipo = 'LEILAO'        :: 'LEILAO' | 'EMISSAO' | integer(), % = 1, enum TipoMensagem
         leilao                 :: ccs:'LicitacaoLeilao'() | undefined, % = 2
         emissao                :: ccs:'SubscricaoTaxaFixa'() | undefined, % = 3
         utilizador             :: iolist()         % = 4
        }).
-endif.

-ifndef('LICITACAOLEILAO_PB_H').
-define('LICITACAOLEILAO_PB_H', true).
-record('LicitacaoLeilao',
        {empresa                :: iolist(),        % = 1
         montante               :: integer(),       % = 2, 32 bits
         taxa                   :: float() | integer() | infinity | '-infinity' | nan % = 3
        }).
-endif.

-ifndef('SUBSCRICAOTAXAFIXA_PB_H').
-define('SUBSCRICAOTAXAFIXA_PB_H', true).
-record('SubscricaoTaxaFixa',
        {empresa                :: iolist(),        % = 1
         montante               :: integer()        % = 2, 32 bits
        }).
-endif.

-ifndef('NOTIFICACAOULTRAPASSADO_PB_H').
-define('NOTIFICACAOULTRAPASSADO_PB_H', true).
-record('NotificacaoUltrapassado',
        {tipo = 'LEILAO'        :: 'LEILAO' | 'EMISSAO' | integer(), % = 1, enum TipoMensagem
         taxa                   :: float() | integer() | infinity | '-infinity' | nan, % = 2
         valor                  :: integer(),       % = 3, 32 bits
         mensagem               :: iolist() | undefined % = 4
        }).
-endif.

-ifndef('RESULTADO_PB_H').
-define('RESULTADO_PB_H', true).
-record('Resultado',
        {tipo = 'LEILAO'        :: 'LEILAO' | 'EMISSAO' | integer(), % = 1, enum TipoMensagem
         empresa                :: iolist(),        % = 2
         texto                  :: iolist()         % = 3
        }).
-endif.

-endif.
