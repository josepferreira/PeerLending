git clone https://github.com/tomas-abrahamsson/gpb.git ../gpb
cd ../gpb 
make
cd ../PeerLending
wget https://s3.amazonaws.com/rebar3/rebar3 && chmod +x rebar3
sudo mv rebar3 /usr/local/bin
sudo apt-get install erlang-jiffy