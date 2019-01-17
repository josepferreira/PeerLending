-module(login_manager).
-export([start/1,login/2,logout/1,online/0]).

% interface functions

start(ListaUsers) ->
        Mapa = carregaMapa(#{},ListaUsers),
        register( login_manager, spawn( fun() -> loop( Mapa ) end ) ).


carregaMapa(M, []) ->
    M
    ;
carregaMapa(M, [H | T]) ->
    Papel = binary_to_list (maps:get(<<"papel">>, H)),
    Password = binary_to_list (maps:get(<<"password">>, H)),
    Username = binary_to_list (maps:get(<<"username">>, H)),
    M1 = maps:put (Username, {Password, false, Papel}, M),
    carregaMapa(M1, T).


login( User, Pass ) ->
    rpc( { login, User, Pass, self() } ).

logout( User ) ->
    io:format("vou desautenticar ~p~n",[User]),
    rpc( { logout, User, self() } ).

online() ->
    rpc( { online, self() } ).

% para generalizar se quisermos podemos fazer com isto, pq são todas iguais!
%logout( User ) -> rpc( { logout,U,self() } ).
rpc( Req ) -> login_manager ! Req,
                receive
                    { login_manager, ok, Papel } -> 
                       Papel;
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
                { ok,{ P,false, Papel } } ->
                    From ! { login_manager, ok, Papel  },
                    loop( maps:put( U,{ P,true, Papel },Map ) );
                _ -> 
                    From ! { login_manager,invalid },
                    loop( Map )
            end;
        { logout, U, From } ->
            case maps:find( U, Map ) of
                { ok,{ P, true, Papel } } ->
                    From ! { login_manager, ok },
                    loop( maps:put( U, { P, false, Papel }, Map ) );
                _ -> 
                    From ! { login_manager, invalid },
                    loop( Map )
            end
    end.
