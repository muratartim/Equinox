'''
Created on 26 Sep 2018

@author: aurora
'''

import socket

# inputs: port number and path to instruction set
portNumber = 1789
inputPath = "/Users/aurora/EclipseWorkspace/Equinox/docs/plugin_api/NewFile.xml"

# create network socket and connect to automation server
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(('localhost', portNumber))

# send send run request
s.sendall('|'.join([inputPath, 'run', '\n']).encode("utf8"))

try:
    
    # wait for server response
    while True:
        
        # receive response
        data = str(s.recv(1024).decode())
        
        # print server response
        print (data)
        
        # get response header
        header = data.split('|')[0]
        
        # completed or failed
        if header in ['Completed', 'Failed']:
            break

# close socket
finally:
    print('closing socket')
    s.close()