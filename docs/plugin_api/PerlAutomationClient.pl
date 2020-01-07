use IO::Socket;

# inputs: host name, port number and path to instruction set
$remote_host = "localhost";
$remote_port = 1789;
$input_path = "/Users/aurora/EclipseWorkspace/Equinox/docs/plugin_api/NewFile.xml";

# create network socket and connect to automation server
$socket = IO::Socket::INET->new(PeerAddr => $remote_host, PeerPort => $remote_port, Proto => "tcp", Type => SOCK_STREAM)
    or die "Couldn't connect to $remote_host:$remote_port : $@\n";

# send send run request
$msg = join "|", $input_path, "run", "\n";
print $socket "$msg";

# wait for server response
while ($data = <$socket>) {

	# print server response
	print $data;
	
	# get response header
	$header = (split(/\|/, $data))[0];
	
	# completed or failed
	if ($header eq "Completed" || $header eq "Failed"){
    	last;
	}
}

# close socket
close($socket);