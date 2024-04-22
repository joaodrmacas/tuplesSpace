import sys
import argparse
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NamingServerServiceImpl import NamingServerServiceImpl
from concurrent import futures

PORT = 5001

def parse_args():
    parser = argparse.ArgumentParser(description="NameServer")
    parser.add_argument('--debug', action='store_true', help="Enable debug mode")
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()
    try:
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))

        pb2_grpc.add_NameServiceServicer_to_server(NamingServerServiceImpl(args.debug), server)

        server.add_insecure_port('[::]:'+str(PORT))

        server.start()

        print("Server listening on port " + str(PORT))

        server.wait_for_termination()

    except KeyboardInterrupt:
        print("NameServer stopped")
        exit(0)
