"""A fake GitHub Packages registry for scripts/test-release.sh, on localhost only.

The registry (HTTPS, first port) stores a PUT once and answers a second PUT of the same path
with 409, as GitHub Packages does; it answers a GET without credentials with 401 and a GET of
a stored file with a redirect to a storage server on another host (127.0.0.1, second port),
which serves the file. Every request is logged as "<server> <method> <path> auth=<who>", where
<who> is "none" or the user name plus which test token was used: never the token itself.

  python3 -I fake_registry.py ROOT REGISTRY_PORT STORAGE_PORT CERT KEY LOG
"""

import base64
import http.server
import os
import ssl
import sys
import threading

root, registry_port, storage_port, cert, key, log = sys.argv[1:7]
registry_port, storage_port = int(registry_port), int(storage_port)


def tls():
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(cert, key)
    return context


def note(server, handler, method):
    header = handler.headers.get("Authorization") or ""
    who = "none"
    if header.startswith("Basic "):
        user, _, token = base64.b64decode(header[6:]).decode().partition(":")
        kind = "publish" if "publish" in token else "read" if "read" in token else "other"
        who = f"{user}:{kind}"
    with open(log, "a") as f:
        f.write(f"{server} {method} {handler.path} auth={who}\n")


def stored(path):
    full = os.path.normpath(os.path.join(root, path.lstrip("/")))
    if not full.startswith(os.path.normpath(root) + os.sep):
        raise ValueError(path)
    return full


class Registry(http.server.BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def answer(self, code, body=b""):
        self.send_response(code)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_PUT(self):
        note("registry", self, "PUT")
        data = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        if not self.headers.get("Authorization"):
            return self.answer(401)
        target = stored(self.path)
        if os.path.exists(target):
            return self.answer(409, b"Conflict: this version exists")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "wb") as f:
            f.write(data)
        self.answer(201)

    def do_GET(self):
        note("registry", self, "GET")
        if not self.headers.get("Authorization"):
            return self.answer(401)
        # A token named "blind" sees nothing: it lets a test reach the registry's own refusal of
        # a second upload, past the client's check that the files are not there yet.
        blind = "blind" in base64.b64decode(self.headers["Authorization"][6:]).decode()
        if blind or not os.path.isfile(stored(self.path)):
            return self.answer(404)
        self.send_response(302)
        self.send_header("Location", f"https://127.0.0.1:{storage_port}/blob{self.path}")
        self.send_header("Content-Length", "0")
        self.end_headers()


class Storage(Registry):
    def do_GET(self):
        note("storage", self, "GET")
        target = stored(self.path[len("/blob"):])
        if not os.path.isfile(target):
            return self.answer(404)
        with open(target, "rb") as f:
            self.answer(200, f.read())


registry = http.server.ThreadingHTTPServer(("localhost", registry_port), Registry)
registry.socket = tls().wrap_socket(registry.socket, server_side=True)
storage = http.server.ThreadingHTTPServer(("127.0.0.1", storage_port), Storage)
storage.socket = tls().wrap_socket(storage.socket, server_side=True)
threading.Thread(target=storage.serve_forever, daemon=True).start()
registry.serve_forever()
