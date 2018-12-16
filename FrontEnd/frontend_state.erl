-module(frontend_state).
-export([start/0]).

-include("ccs.hrl").

%Neste modulo vao ser implementadas funcoes de comunicacao com exchange e gestao da logica dessa comunicacao
% MapEstado :  empresa -> lista de objetos apostador (username, pid)
% MapUser : username -> papel


start() ->
  io:format("State ja esta a correr!"),
  register ( frontend_state, spawn (fun() -> loop (#{}, #{"emp1" => "empresa", "emp2" => "empresa", "cli1" => "cliente", "cli2" => "cliente"}) end ))
.

loop (MapUser, MapEstado) -> 
    receive
        {getPapel, User, From} ->
            case maps:find(User, MapUser) of
                {ok, Papel} ->
                    From ! Papel,
                    loop(MapUser, MapEstado);
                _ -> 
                    From ! invalid,
                    loop(MapUser, MapEstado)
                
            end
    end
.



rpc( Req ) ->
    frontend_state ! Req,
    receive
        {frontend_state, Res } -> Res
    end
.

getPapel (User) ->
    rpc ({getPapel, User})
.

