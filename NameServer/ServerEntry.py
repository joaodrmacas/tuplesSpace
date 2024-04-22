class ServerEntry:
    def __init__(self, host, port, qualifier):
        self.host = host
        self.port = port
        self.qualifier = qualifier

    def __str__(self):
        return f"Server: {self.host}:{self.port}, Qualifier: {self.qualifier}"