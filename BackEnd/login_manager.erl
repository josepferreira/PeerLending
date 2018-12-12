-module(login_manager).
-export([start/0,login/2,logout/1,online/0]).

% interface functions

start() ->
        Mapa = carregaMapa(#{}),
        register( login_manager,spawn( fun() -> loop( Mapa ) end ) ).

carregaMapa(Map) ->
    M1 = maps:put( "emp1",{ "123",false },Map ),
    M2 = maps:put( "emp2",{ "123",false },M1 ),
    M3 = maps:put( "cli1",{ "123",false },M2 ),
    M4 = maps:put( "cli2",{ "123",false },M3 ),
    M4.

login( User,Pass ) ->
    rpc( { login,User,Pass,self() } ).

logout( User ) ->
    io:format("vou desautenticar ~p~n",[User]),
    rpc( { logout,User,self() } ).

online() ->
    rpc( { online,self() } ).

% para generalizar se quisermos podemos fazer com isto, pq são todas iguais!
%logout( User ) -> rpc( { logout,U,self() } ).
rpc( Req ) -> login_manager ! Req,
                receive{ login_manager,Res } -> Res end.
% process

loop( Map ) ->
    receive
        { login,U,P,From } ->
            case maps:find( U,Map ) of
                { ok,{ P,false } } ->
                    From ! { login_manager,ok },
                    loop( maps:put( U,{ P,true },Map ) );
                _ -> 
                    From ! { login_manager,invalid },
                    loop( Map )
            end;
        { logout,U,From } ->
            case maps:find( U,Map ) of
                { ok,{ P,true } } ->
                    From ! { login_manager,ok },
                    loop( maps:put( U,{ P,false },Map ) );
                _ -> 
                    From ! { login_manager,invalid },
                    loop( Map )
            end;
        { online,From } ->
                Predicado = fun( _,{ _,V2 } ) -> V2 == true end,
                NovoMap = maps:filter( Predicado,Map ),
                From ! { login_manager,maps:keys(NovoMap) },
                loop( Map )
    end.
