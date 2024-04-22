from Locks import RWLock

class ServiceEntry:
    def __init__(self, service_name):
        self.service_name = service_name
        self.servers = []
        self.lock = RWLock()

    def add_server(self, server):
        self.lock.acquire_write()
        ret_val = -1
        try:
            self.servers.append(server)
            ret_val = 0
        finally:
            self.lock.release_write()
        return ret_val

    def delete_server(self, server_address):
        self.lock.acquire_write()
        ret_val = -1
        try:
            host, port = server_address.split(":")
            for server in self.servers:
                if server.host == host and server.port == port:
                    self.servers.remove(server)
                    ret_val = 0
                    break

        finally:
            self.lock.release_write()
        return ret_val

    def __str__(self):
        self.lock.acquire_read()
        try:
            servers_info = "\n".join([str(server) for server in self.servers])
        finally:
            self.lock.release_read()
        return f"Service: {self.service_name}\nServers:\n{servers_info}"
