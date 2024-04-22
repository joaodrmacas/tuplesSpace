from ServiceEntry import ServiceEntry
from Locks import RWLock
from ServerEntry import ServerEntry
import sys


class NamingServer:
    def __init__(self, debug):
        self.service_map = {}
        self.lock = RWLock()
        self.debug_flag = debug


    def __register_service(self, service_name):
        '''This is meant to be a private method! Do not call it outside of the class in order to assure thread safety'''
        self.debug(f"Registering service with name: \"{service_name}\" on name server")
        try:
            if service_name not in self.service_map:
                self.service_map[service_name] = ServiceEntry(service_name)
                self.debug(f"Service with name: \"{service_name}\" registered successfully")
                return 0
        except Exception as e:
            self.debug(f"Exception occurred while registering service: {e}")
            return -1
        self.debug(f"Service with name: \"{service_name}\" already existed")
        return 0


    def register_server(self, service_name, server_host, server_port, server_qualifier):
        self.lock.acquire_write()
        reg_result = -1
        try:
            for service in self.service_map.values():
                for server in service.servers:
                    if server.qualifier == server_qualifier:
                        self.debug(f"Server with qualifier \"{server_qualifier}\" already exists.")
                        return -1
                    elif server.host == server_host and server.port == server_port:
                        self.debug(f"Server with host \"{server_qualifier}\" and port \"{server_port}\" already exists.")
                        return -1


            server = ServerEntry(server_host, server_port, server_qualifier)
            self.debug(f"Successfully created server entry for server \"{str(server)}\"")
            reg_result = self.__register_service(service_name)
            if service_name in self.service_map:
                if self.service_map[service_name].add_server(server) == -1:
                    reg_result = -1
            else:
                reg_result = -1
        finally:
            self.lock.release_write()

            self.debug(f"Failed to register server \"{str(server)}\"") if reg_result == -1 \
            else self.debug(f"Successfully registered server \"{server_host}:{server_port}\"")

        return reg_result

    def lookup(self, service_name, server_qualifier=""):
        self.lock.acquire_read()
        try:
            self.debug(f"Looking for servers within the service \"{service_name}\"")
            lst = []
            if service_name in self.service_map:
                for server in self.service_map[service_name].servers:
                    if server_qualifier == "" or server.qualifier == server_qualifier:
                        lst.append(server.host + ":" + server.port)
            self.debug(f"Found servers: {[str(server) for server in lst]}")
        finally:
            self.lock.release_read()
        return lst


    def delete(self, service_name, server_address):
        self.debug(f"Trying to delete server \"{server_address}\"")
        if service_name not in self.service_map:
            self.debug(f"Delete failed, no service with name \"{service_name}\"")
            return -1
        ret_res = self.service_map[service_name].delete_server(server_address)
        self.debug(f"Delete failed, no server found with address \"{server_address}\"") if ret_res == -1 \
            else self.debug(f"Delete succsessfull for server \"{server_address}\"")
        return ret_res

    def debug(self, message):
        if self.debug_flag:
            print(message, file=sys.stderr)


    def __str__(self):
        self.lock.acquire_read()
        try:
            service_entries = [str(service_entry) for service_entry in self.service_map.values()]
        finally:
            self.lock.release_read()
        return "\n".join(service_entries)
    
