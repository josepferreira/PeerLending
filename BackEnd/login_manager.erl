-module(login_manager).
-export([start/0, create_account/2,close_account/2,login/2,logout/1,online/0]).

% interface functions

start() ->
        register( login_manager,spawn( fun() -> loop( #{} ) end ) ).

create_account( User,Pass ) ->
    rpc( { create_account,User,Pass,self() } ).

close_account( User,Pass ) ->
    rpc( { close_account,User,Pass,self() } ).

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
        { create_account,U,P,From } -> 
            case maps:find( U,Map ) of
                error ->
                    From ! { login_manager,ok },
                    loop( maps:put( U,{ P,true },Map ) );
                
                _ -> 
                    From ! { login_manager,user_exists },
                    loop( Map )
            end;
        
        { close_account,U,P,From } ->
            case maps:find( U,Map ) of
                { ok,{ P,_ } } -> % se existir uma entrada e a password for igual então dá sucesso, senao não dá
                    From ! { login_manager,ok },
                    loop( maps:remove( U,Map ) );
                _ -> 
                    From ! { login_manager,error },
                    loop( Map )
            end;
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
