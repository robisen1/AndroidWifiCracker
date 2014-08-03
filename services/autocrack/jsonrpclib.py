# Based on the json-rpc specs: http://json-rpc.org/wiki/specification
# The main deviation is on the error treatment. The official spec
# would set the 'error' attribute to a string. This implementation
# sets it to a dictionary with keys: message/traceback/type

import json
import SocketServer
import sys
import traceback
import time
try:
    import fcntl
except ImportError:
    fcntl = None


###
### Server code
###
import SimpleXMLRPCServer
import SimpleHTTPServer
import urlparse

list_public_methods = SimpleXMLRPCServer.list_public_methods

class SimpleJSONRPCDispatcher(SimpleXMLRPCServer.SimpleXMLRPCDispatcher):
    def _marshaled_dispatch(self, data, dispatch_method = None):
        id = None
        try:
            req = json.loads(data)
            method = req['method']
            params = req['params']
            id     = req['id']

            if dispatch_method is not None:
                result = dispatch_method(method, params)
            else:
                result = self._dispatch(method, params)
            response = dict(id=id, result=result, error=None)
        except:
            extpe, exv, extrc = sys.exc_info()
            err = dict(type=str(extpe),
                       message=str(exv),
                       traceback=''.join(traceback.format_tb(extrc)))
            response = dict(id=id, result=None, error=err)
        try:
            return json.dumps(response)
        except:
            extpe, exv, extrc = sys.exc_info()
            err = dict(type=str(extpe),
                       message=str(exv),
                       traceback=''.join(traceback.format_tb(extrc)))
            response = dict(id=id, result=None, error=err)
            return json.dumps(response)


class SimpleJSONRPCRequestHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
    # Class attribute listing the accessible path components;
    # paths not on this list will result in a 404 error.
    rpc_paths = ('/', '/JSON')
    
    def report_404 (self):
            # Report a 404 error
        self.send_response(404)
        response = 'No such page'
        self.send_header("Content-type", "text/plain")
        self.send_header("Content-length", str(len(response)))
        self.end_headers()
        self.wfile.write(response)
        # shut down the connection
        self.wfile.flush()
        self.connection.shutdown(1)    
    
    def is_rpc_path_valid(self):
        if self.rpc_paths:
            return self.path in self.rpc_paths
        else:
            # If .rpc_paths is empty, just assume all paths are legal
            return True    
    
    def do_POST(self):
        """Handles the HTTP POST request.

        Attempts to interpret all HTTP POST requests as JSON-RPC calls,
        which are forwarded to the server's _dispatch method for handling.
        """

        # Check that the path is legal
        if not self.is_rpc_path_valid():
            self.report_404()
            return

        try:
            # Get arguments by reading body of request.
            # We read this in chunks to avoid straining
            # socket.read(); around the 10 or 15Mb mark, some platforms
            # begin to have problems (bug #792570).
            max_chunk_size = 10*1024*1024
            size_remaining = int(self.headers["content-length"])
            L = []
            while size_remaining:
                chunk_size = min(size_remaining, max_chunk_size)
                L.append(self.rfile.read(chunk_size))
                size_remaining -= len(L[-1])
            data = ''.join(L)
            sys.stderr.write("%s: %s" % (time.asctime(), data))
            # In previous versions of SimpleXMLRPCServer, _dispatch
            # could be overridden in this class, instead of in
            # SimpleXMLRPCDispatcher. To maintain backwards compatibility,
            # check to see if a subclass implements _dispatch and dispatch
            # using that method if present.
            response = self.server._marshaled_dispatch(
                    data, getattr(self, '_dispatch', None)
                )
        except: # This should only happen if the module is buggy
            # internal error, report as HTTP server error
            self.send_response(500)
            self.end_headers()
        else:
            # got a valid XML RPC response
            self.send_response(200)
            self.send_header("Content-type", "application/json-rpc")
            self.send_header("Content-length", str(len(response)))
            self.end_headers()
            self.wfile.write(response)

            # shut down the connection
            self.wfile.flush()
            self.connection.shutdown(1)

class SimpleJSONRPCServer(SocketServer.TCPServer,
                          SimpleJSONRPCDispatcher):
    """Simple JSON-RPC server.

    Simple JSON-RPC server that allows functions and a single instance
    to be installed to handle requests. The default implementation
    attempts to dispatch JSON-RPC calls to the functions or instance
    installed in the server. Override the _dispatch method inhereted
    from SimpleJSONRPCDispatcher to change this behavior.
    """

    allow_reuse_address = True

    def __init__(self, addr, requestHandler=SimpleJSONRPCRequestHandler,
                 logRequests=True):
        self.logRequests = logRequests

        SimpleJSONRPCDispatcher.__init__(self, allow_none=True, encoding=None)
        SocketServer.TCPServer.__init__(self, addr, requestHandler)

        # [Bug #1222790] If possible, set close-on-exec flag; if a
        # method spawns a subprocess, the subprocess shouldn't have
        # the listening socket open.
        if fcntl is not None and hasattr(fcntl, 'FD_CLOEXEC'):
            flags = fcntl.fcntl(self.fileno(), fcntl.F_GETFD)
            flags |= fcntl.FD_CLOEXEC
            fcntl.fcntl(self.fileno(), fcntl.F_SETFD, flags)


###
### Client code
###
import xmlrpclib, httplib


class ResponseError(xmlrpclib.ResponseError):
    pass
class Fault(xmlrpclib.ResponseError):
    pass

def _get_response(file, sock):
    data = ""
    while 1:
        if sock:
            response = sock.recv(1024)
        else:
            response = file.read(1024)
        if not response:
            break
        data += response

    file.close()

    return data

class Transport(xmlrpclib.Transport):
    def _parse_response(self, file, sock):
        return _get_response(file, sock)

class SafeTransport(xmlrpclib.SafeTransport):
    def _parse_response(self, file, sock):
        return _get_response(file, sock)

class TimeoutTransport(Transport):

    def make_connection(self, host):
        conn = TimeoutHTTP(host)
        conn.set_timeout(self.timeout)
        return conn

class TimeoutHTTPConnection(httplib.HTTPConnection):

    def connect(self):
        httplib.HTTPConnection.connect(self)
        self.sock.settimeout(self.timeout)        

class TimeoutHTTP(httplib.HTTP):
    _connection_class = TimeoutHTTPConnection

    def set_timeout(self, timeout):
        self._conn.timeout = timeout


class ServerProxy:
    def __init__(self, uri, id=None, transport=None, use_datetime=0):
        # establish a "logical" server connection

        # get the url
        import urllib
        type, uri = urllib.splittype(uri)
        if type not in ("http", "https"):
            raise IOError, "unsupported JSON-RPC protocol"
        self.__host, self.__handler = urllib.splithost(uri)
        if not self.__handler:
            self.__handler = "/JSON"

        if transport is None:
            if type == "https":
                transport = SafeTransport(use_datetime=use_datetime)
            else:
                transport = Transport(use_datetime=use_datetime)

        self.__transport = transport
        self.__id        = id

    def __request(self, methodname, params):
        # call a method on the remote server

        request = json.dumps(dict(id=self.__id, method=methodname,
                                    params=params))

        data = self.__transport.request(
            self.__host,
            self.__handler,
            request,
            verbose=False
            )

        response = json.loads(data)

        if response["id"] != self.__id:
            raise ResponseError("Invalid request id (is: %s, expected: %s)" \
                                % (response["id"], self.__id))
        if response["error"] is not None:
            raise Fault("JSON Error", response["error"])
        return response["result"]

    def __repr__(self):
        return (
            "<ServerProxy for %s%s>" %
            (self.__host, self.__handler)
            )

    __str__ = __repr__

    def __getattr__(self, name):
        # magic method dispatcher
        return xmlrpclib._Method(self.__request, name)

def Server(url, *args, **kwargs):
    t = TimeoutTransport()
    t.timeout = kwargs.get('timeout', 120)
    if 'timeout' in kwargs:
        del kwargs['timeout']
    kwargs['transport'] = t
    server = ServerProxy(url, *args, **kwargs)
    return server

if __name__ == '__main__':
    if not len(sys.argv) > 1:
        print 'Running JSON-RPC server on port 8000'
        server = SimpleJSONRPCServer(("localhost", 8000))
        server.register_function(pow)
        server.register_function(lambda x,y: x+y, 'add')
        server.serve_forever()
    else:
        remote = ServerProxy(sys.argv[1])
        print 'Using connection', remote

        print repr(remote.add(1, 2))
        aaa = remote.add
        print repr(remote.pow(2, 4))
        print aaa(5, 6)
        
        print "ians "
        print remote.getName()
        
        try:
            # Invalid parameters
            aaa(5, "toto")
            print "Successful execution of invalid code"
        except Fault:
            pass

        try:
            # Invalid parameters
            aaa(5, 6, 7)
            print "Successful execution of invalid code"
        except Fault:
            pass

        try:
            # Invalid method name
            print repr(remote.powx(2, 4))
            print "Successful execution of invalid code"
        except Fault:
            pass