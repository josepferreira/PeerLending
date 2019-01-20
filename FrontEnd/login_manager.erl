-module(login_manager).
-export([start/1,login/2,logout/1,online/0,adicionaSubscricaoEmpresa/2,retiraSubscricaoEmpresa/2,alteraSubscricaoEmissao/2,alteraSubscricaoLeilao/2]).

% interface functions

start(ListaUsers) ->
        Mapa = carregaMapa(#{},ListaUsers),
        register( login_manager, spawn( fun() -> loop( Mapa ) end ) ).


carregaMapa(M, []) ->
    io:format("~p~n", [M]),
    M
    ;
carregaMapa(M, [H | T]) ->
    Papel = binary_to_list (maps:get(<<"papel">>, H)),
    Password = binary_to_list (maps:get(<<"password">>, H)),
    Username = binary_to_list (maps:get(<<"username">>, H)),
    M1 = maps:put (Username, {Password, false, Papel,false,false,[]}, M),
    carregaMapa(M1, T).

eEmpresa(Elem, Map)->
    Pred = fun(_,{_, _, Papel,_,_,_}) -> Papel == "empresa" end,
    MapaEmpresas = maps:filter(Pred, Map),
    Boolean = maps:is_key(Elem, MapaEmpresas ),
    Boolean.


login( User, Pass ) ->
    rpc( { login, User, Pass, self() } ).

logout( User ) ->
    io:format("vou desautenticar ~p~n",[User]),
    rpc( { logout, User, self() } ).

online() ->
    rpc( { online, self() } ).

adicionaSubscricaoEmpresa( User,Emp ) ->
    rpc( {adicionaSubscricaoEmpresa, User, Emp, self() } ).

retiraSubscricaoEmpresa( User,Emp ) ->
    rpc( {retiraSubscricaoEmpresa, User, Emp, self() } ).

alteraSubscricaoEmissao( User,Emi ) ->
    rpc( {alteraSubscricaoEmissao, User, Emi, self() } ).

alteraSubscricaoLeilao( User,Lei ) ->
    rpc( {alteraSubscricaoLeilao, User, Lei, self() } ).

% para generalizar se quisermos podemos fazer com isto, pq são todas iguais!
%logout( User ) -> rpc( { logout,U,self() } ).
rpc( Req ) -> login_manager ! Req,
                receive
                    { login_manager, ok, Papel, E, L, List } -> 
                       {Papel, E, L, List};
                    { login_manager, ok } -> 
                       ok;
                    {login_manager, invalid } ->
                        invalid
                    end
                .
% process

loop( Map ) ->
    receive
        { login, U, P, From } ->
            case maps:find( U,Map ) of
                { ok,{ P,false, Papel,E,L,List } } ->
                    From ! { login_manager, ok, Papel,E,L,List },
                    loop( maps:put( U,{ P,true, Papel,E,L,List },Map ) );
                _ -> 
                    From ! { login_manager,invalid },
                    loop( Map )
            end;
        { logout, U, From } ->
            case maps:find( U, Map ) of
                { ok,{ P, true, Papel,E,L,List } } ->
                    From ! { login_manager, ok },
                    loop( maps:put( U, { P, false, Papel,E,L,List }, Map ) );
                _ -> 
                    From ! { login_manager, invalid },
                    loop( Map )
            end;
        { adicionaSubscricaoEmpresa, U, Emp, From } ->
            case maps:find(U, Map) of
                {ok, {P, true, Papel, Emissao, Leilao, List}} ->
                    From ! {login_manager, ok},
                    case lists:member(Emp, List) of
                        false ->
                            case eEmpresa(Emp, Map) of
                                true ->
                                        io:format("Vou adicionar emp sub~n"),
                                        io:format("Lista de Subscricoes de ~p : ~p~n", [U, [Emp | List]]),
                                        loop (maps:put (U, {P, true, Papel, Emissao, Leilao, [Emp | List] },Map));
                                    _ ->
                                        io:format("~p nao e uma EMPRESA!!!~n", [Emp]),
                                        loop(Map)
                            end;
                        _ -> 
                            io:format("~p nao existe no SISTEMA!!!~n", [Emp]),
                            loop(Map)
                    end;
                _ ->
                    From ! {login_manager, invalid},
                    loop(Map)
            end;
        { retiraSubscricaoEmpresa, U, Emp, From } ->
            case maps:find(U, Map) of
                {ok, {P, true, Papel, Emissao, Leilao, List}} ->
                    From ! {login_manager, ok},
                    loop (maps:put (U, {P, true, Papel, Emissao, Leilao, lists:delete(Emp,List) },Map));
                _ ->
                    From ! {login_manager, invalid},
                    loop(Map)
            end;
        { alteraSubscricaoEmissao, U, Emi, From } ->
            case maps:find(U, Map) of
                {ok, {P, true, Papel, _, Leilao, List}} ->
                    From ! {login_manager, ok},
                    loop (maps:put (U, {P, true, Papel, Emi, Leilao, List },Map));
                _ ->
                    From ! {login_manager, invalid},
                    loop(Map)
            end;
        { alteraSubscricaoLeilao, U, Lei, From } ->
            case maps:find(U, Map) of
                {ok, {P, true, Papel, Emissao, _, List}} ->
                    From ! {login_manager, ok},
                    loop (maps:put (U, {P, true, Papel, Emissao, Lei,  List },Map));
                _ ->
                    From ! {login_manager, invalid},
                    loop(Map)
            end
    end.
