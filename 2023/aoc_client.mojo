import os
from pathlib import Path

from steflib.socket import getaddrinfo, TCPSocket


alias DATA_DIR = Path(".data")


fn data_url(year: Int, day: Int) -> String:
    return "https://adventofcode.com/" + str(year) + "/day/" + str(day) + "/input"


# TODO(issue): Tuple.get only supports AnyRegType
@value
struct Header(CollectionElement):
    var name: String
    var value: String


# TODO(issue): can't return form inside `with`
# TODO(issue): can't use a VariadicListMem of Header
fn http_get(url: String, headers: DynamicVector[Header]) raises -> String:
    let protocol: String
    let host: String
    let path: String
    var parse_i: Int = 0

    let protocol_index = url.find("://")
    if protocol_index >= 0:
        protocol = url[:protocol_index]
        parse_i = protocol_index + len("://")
    else:
        protocol = "http"

    # TODO(issue): there's a crash here if parse_i is 4
    let path_index = url.find("/", start=parse_i)
    if path_index >= 0:
        host = url[parse_i : parse_i + path_index]
        path = url[parse_i + path_index :]
        if url.find("?") >= 0:
            raise "URL params not yet supported"
    else:
        host = url[parse_i:]
        path = "/"

    # TODO(me): this breaks with ("adventofc", "http")
    let address = getaddrinfo(host._strref_dangerous(), protocol._strref_dangerous())

    var request_body = "GET " + path + " HTTP/1.0"
    request_body += "\nHost: " + host
    for i in range(len(headers)):
        let header = headers[i]
        request_body += "\n" + header.name + ": " + header.value
    request_body += "\n\n"

    host._strref_keepalive()
    protocol._strref_keepalive()

    # TODO(issue): `with` doesn't think this is initialized
    var response: String = ""
    with TCPSocket.connect(address) as socket:
        socket.send(request_body._strref_dangerous())
        request_body._strref_keepalive()
        response = socket.recv()
    return response  # TODO LOL


def main():
    let session_key = os.getenv("AOC_SESSION")
    if not session_key:
        raise "AOC_SESSION not set"
    var headers = DynamicVector[Header]()
    # TODO(issue): String.__add__(StringRef)
    headers.push_back(Header("Cookie", "session=" + String(session_key)))
    let response_body = http_get(data_url(year=2023, day=1), headers)
    print(response_body)
