import sys
from grpc import StatusCode

sys.path.insert(1, '../contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NamingServer import NamingServer

class NamingServerServiceImpl(pb2_grpc.NameServiceServicer):

    def __init__(self, debug):
        self.naming_server = NamingServer(debug)

    def register(self, request, context):
        service_name = request.service_name
        server_host, server_port = request.server_address.split(":")
        server_qualifier = request.server_qualifier
        register_result = self.naming_server.register_server(service_name, server_host, server_port, server_qualifier)
        if register_result == -1:
            context.abort(StatusCode.INTERNAL, "Not possible to register the server")
        return pb2.RegisterResponse()

    def lookup(self, request, context):
        service_name = request.service_name
        server_qualifier = request.server_qualifier
        try:
            server_lst = self.naming_server.lookup(service_name, server_qualifier)
        except:
            context.abort(StatusCode.INTERNAL, "Could not communicate with the name server")

        return pb2.LookupResponse(servers=server_lst)
    
    def delete(self, request, context):
        service_name = request.service_name
        server_address = request.server_address
        deletion_result = self.naming_server.delete(service_name, server_address)

        if deletion_result == -1:
            context.abort(StatusCode.INTERNAL, "Not possible to remove the server")
        else:
            return pb2.DeleteResponse()
