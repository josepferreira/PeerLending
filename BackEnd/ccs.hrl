%% -*- coding: utf-8 -*-
%% Automatically generated, do not edit
%% Generated by gpb_compile version 4.4.0

-ifndef(ccs).
-define(ccs, true).

-define(ccs_gpb_version, "4.4.0").

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

-endif.