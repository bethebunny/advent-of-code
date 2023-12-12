from math import min, max
from memory.anypointer import AnyPointer
from sys import external_call
from .variant import Variant


fn perror(message: StringRef):
    external_call["perror", NoneType](message.data)


@value
@register_passable("trivial")
struct AddressInfo:
    var value: Int32

    alias PASSIVE = Self {value: 1}
    alias CANONNAME = Self {value: 2}
    alias NUMERICHOST = Self {value: 4}
    alias ALL = Self {value: 256}
    alias V4MAPPED_CFG = Self {value: 512}
    alias ADDRCONFIG = Self {value: 1024}
    alias V6MAPPED = Self {value: 2048}
    alias NUMERICSERV = Self {value: 4096}

    fn __or__(self, other: Self) -> Self:
        return Self {value: self.value | other.value}

    fn __eq__(self, other: Self) -> Bool:
        return self.value == other.value

    fn __contains__(self, other: Self) -> Bool:
        # TODO(issue): Bool()
        return (self.value & other.value).__bool__()


@value
@register_passable("trivial")
struct AddressFamily:
    var value: Int32

    alias UNSPEC = Self {value: 0}
    alias UNIX = Self {value: 1}
    alias INET = Self {value: 2}
    alias SNA = Self {value: 11}
    alias APPLETALK = Self {value: 16}
    alias ROUTE = Self {value: 17}
    alias LINK = Self {value: 18}
    alias IPX = Self {value: 23}
    alias INET6 = Self {value: 30}
    alias SYSTEM = Self {value: 32}

    fn __eq__(self, other: Self) -> Bool:
        return self.value == other.value


@value
@register_passable("trivial")
struct SocketKind:
    var value: Int32

    alias STREAM = Self {value: 1}
    alias DGRAM = Self {value: 2}
    alias RAW = Self {value: 3}
    alias RDM = Self {value: 4}
    alias SEQPACKET = Self {value: 5}

    fn __eq__(self, other: Self) -> Bool:
        return self.value == other.value


@value
@register_passable("trivial")
struct Protocol:
    var value: Int32

    alias DEFAULT = Self {value: 0}


@register_passable("trivial")
struct CStringRef(Sized, Stringable):
    var data: DTypePointer[DType.int8]

    fn __init__() -> Self:
        return Self {data: DTypePointer[DType.int8]()}

    fn __init__(data: DTypePointer[DType.int8]) -> Self:
        return Self {data: data}

    fn stringref(self) -> StringRef:
        return StringRef(self.data, len(self))

    fn __str__(self) -> String:
        return String(self.stringref())

    fn __len__(self) -> Int:
        if not self.data:
            return 0
        var len = 0
        while self.data[len]:
            len += 1
        return len


alias SocketAddress = Variant[IPV4SocketAddress, IPV6SocketAddress]


@value
@register_passable
struct IPV4SocketAddress(Stringable, CollectionElement):
    # struct sockaddr_in {
    #     short int          sin_family;  // Address family, AF_INET
    #     unsigned short int sin_port;    // Port number
    #     struct in_addr     sin_addr;    // Internet address
    #     unsigned char      sin_zero[8]; // Same size as struct sockaddr
    # };
    var _family: Int16
    var port: Int16
    var address: StaticTuple[4, Int8]
    var _padding: Int64

    fn family(self) -> AddressFamily:
        return AddressFamily {value: self._family.cast[DType.int32]()}

    fn __str__(self) -> String:
        return (
            str(self.address[0].to_int() % 256)
            + "."
            + str(self.address[1].to_int() % 256)
            + "."
            + str(self.address[2].to_int() % 256)
            + "."
            + str(self.address[3].to_int() % 256)
            + ":"
            + str(self.port)
        )


@value
@register_passable
struct IPV6SocketAddress(Stringable, CollectionElement):
    # struct sockaddr_in6 {
    #     u_int16_t       sin6_family;   // address family, AF_INET6
    #     u_int16_t       sin6_port;     // port number, Network Byte Order
    #     u_int32_t       sin6_flowinfo; // IPv6 flow information
    #     struct in6_addr sin6_addr;     // IPv6 address
    #     u_int32_t       sin6_scope_id; // Scope ID
    # };

    # struct in6_addr {
    #     unsigned char   s6_addr[16];   // IPv6 address
    # };
    var _family: Int16
    var port: Int16
    var flow_info: Int32
    var address: StaticTuple[16, Int8]
    var scope_id: Int32

    fn family(self) -> AddressFamily:
        return AddressFamily {value: self._family.cast[DType.int32]()}

    fn __str__(self) -> String:
        # TODO(issue): hex()
        return "Address(inet6...(TODO))"
        # var s = String()
        # for i in range(16):
        #     if i > 0 and i % 2 == 0:
        #         s += ":"
        #     let s = self.address[0]
        #     s += hex(self.address[i])


@value
struct Address(Stringable):
    var family: AddressFamily
    var length: Int16
    var data: SocketAddress

    fn pointer(inout self) -> Pointer[SocketAddress]:
        return Pointer.address_of(self.data)

    # TODO(issue): variant should inherit and dispatch shared traits
    fn __str__(self) -> String:
        if self.family == AddressFamily.INET:
            debug_assert(self.data.isa[IPV4SocketAddress](), "mismatched address type")
            var data = self.data  # need to make a copy :(
            return str(data.get[IPV4SocketAddress]())
        elif self.family == AddressFamily.INET6:
            debug_assert(self.data.isa[IPV6SocketAddress](), "mismatched address type")
            var data = self.data  # need to make a copy :(
            return str(data.get[IPV6SocketAddress]())
        return "Address(family=" + str(self.family.value) + ")"


# Naive Movable/Copyable is definitely bad for modeling this type.
# Basically it should never be moved if `next` is populated, since
# then we'll potentially free arbitrary memory in `freeaddrinfo`.
# The "right" way to model this is probably to have 3 types:
# - This type, which is unsafe and doesn't implement Move/Copy
# - A parallel type without a `next` which has value semantics
# - A list type of the 2nd type, which is implemented in its interior
#   via the 1st type.
# We'll leave this for now because I want to get to more socket stuff :P
@value
struct _AddrInfo(Movable, Stringable):
    # struct addrinfo {
    #     int              ai_flags;     // AI_PASSIVE, AI_CANONNAME, etc.
    #     int              ai_family;    // AF_INET, AF_INET6, AF_UNSPEC
    #     int              ai_socktype;  // SOCK_STREAM, SOCK_DGRAM
    #     int              ai_protocol;  // use 0 for "any"
    #     size_t           ai_addrlen;   // size of ai_addr in bytes
    #     struct sockaddr *ai_addr;      // struct sockaddr_in or _in6
    #     char            *ai_canonname; // full canonical hostname

    #     struct addrinfo *ai_next;      // linked list, next node
    # };
    # TODO(issue): default values
    # TODO(issue): can't declare `AnyPointer[Self]`
    var flags: AddressInfo
    var family: AddressFamily
    var type: SocketKind
    var protocol: Protocol
    var address_length: Int64

    # These _should_ be [address, canonical_hostname] but OSX reverses them (!!!)
    # TODO(issue): We don't have a good way to conditionally define struct fields,
    #              so name these something nonsense for now.
    var _pointer1: Pointer[NoneType]
    var _pointer2: Pointer[NoneType]

    # @parameter
    # if sys.info.os_is_macos():
    #     var canonical_hostname: CStringRef
    #     var _address: Pointer[SocketAddress]
    # else:
    #     var _address: Pointer[SocketAddress]
    #     var canonical_hostname: CStringRef

    var _next: Pointer[NoneType]  # Actually a pointer to _AddrInfo

    fn __init__(
        inout self,
        flags: AddressInfo = AddressInfo.PASSIVE,
        family: AddressFamily = AddressFamily.UNSPEC,
        type: SocketKind = SocketKind.STREAM,
        protocol: Protocol = Protocol.DEFAULT,
        address_length: Int = 0,
    ):
        self.flags = flags
        self.family = family
        self.type = type
        self.protocol = protocol
        self.address_length = address_length
        # self._address = Pointer[SocketAddress]()
        # self.canonical_hostname = CStringRef()
        self._pointer1 = Pointer[NoneType]()
        self._pointer2 = Pointer[NoneType]()
        self._next = Pointer[NoneType]()

    # TODO(issue): default implementation of __str__ for @value types
    fn __str__(self) -> String:
        return (
            "AddrInfo("
            + "\n\tflags="
            + str(self.flags.value)
            + ","
            + "\n\tfamily="
            + str(self.family.value)
            + ","
            + "\n\ttype="
            + str(self.type.value)
            + ","
            + "\n\tprotocol="
            + str(self.protocol.value)
            + ","
            + "\n\taddress_length="
            + str(self.address_length)
            + ","
            + "\n\taddress_ptr="
            + str(self.address())
            + ","
            + "\n\thostname='"
            + str(self.canonical_hostname())
            + "',"
            + "\n\tnext="
            + str(self._next.__as_index())
            + ","
            + "\n)"
        )

    fn canonical_hostname(self) -> String:
        let ptr: Pointer[NoneType]

        @parameter
        if sys.info.os_is_macos():
            ptr = self._pointer1
        else:
            ptr = self._pointer2
        let cref = CStringRef(DTypePointer[DType.int8](ptr.bitcast[Int8]()))
        # return str(cref)  # TODO(issue): parser crash
        return cref.__str__()

    fn _address(self) -> Pointer[SocketAddress._type]:
        let ptr: Pointer[NoneType]

        @parameter
        if sys.info.os_is_macos():
            ptr = self._pointer2
        else:
            ptr = self._pointer1
        return ptr.bitcast[SocketAddress._type]()

    fn has_next(self) -> Bool:
        return self._next.__bool__()

    fn next(self) -> Self:
        return __get_address_as_lvalue(self._next.bitcast[Self]().address)

    fn address(self) -> Address:
        let data: SocketAddress
        if self.family == AddressFamily.INET:
            data = SocketAddress(
                __get_address_as_lvalue(self._address().address),
                SocketAddress._check[IPV4SocketAddress](),
            )
        elif self.family == AddressFamily.INET6:
            data = SocketAddress(
                __get_address_as_lvalue(self._address().address),
                SocketAddress._check[IPV6SocketAddress](),
            )
        else:
            print("Unsupported address family: " + str(self.family.value))
            trap()
            data = SocketAddress(
                __get_address_as_lvalue(self._address().address),
                SocketAddress._check[IPV4SocketAddress](),
            )
        return Address(self.family, self.address_length.cast[DType.int16](), data)


fn getaddrinfo(node: StringRef, service: StringRef) raises -> Address:
    var hints: _AddrInfo = _AddrInfo(family=AddressFamily.INET)
    var results = AnyPointer[_AddrInfo]()

    let result = external_call["getaddrinfo", Int32](
        node.data,
        service.data,
        Pointer.address_of(hints),
        Pointer.address_of(results),
    )

    if result == -1:
        perror("getaddrinfo")
        raise "Failed getaddrinfo"

    print(__get_address_as_lvalue(results.value))

    let final = __get_address_as_lvalue(results.value).address()

    external_call["freeaddrinfo", NoneType](results)

    return final


alias SocketFD = Int32


@value
struct TCPSocket:
    var address: Address
    var fd: SocketFD

    fn __init__(inout self, owned address: Address) raises:
        self.fd = external_call["socket", SocketFD](
            address.family,
            SocketKind.STREAM,
            0,  # default protocol
        )
        if self.fd == -1:
            perror("socket create")
            raise "Failed to create socket from address:" + str(address)
        self.address = address ^

    @staticmethod
    fn connect(owned address: Address) raises -> Self:
        let socket = Self(address)
        socket._connect()
        return socket

    fn _connect(self) raises:
        var address = self.address  # copy so we can take a pointer
        let connect_result = external_call["connect", Int32](
            self.fd, address.pointer(), self.address.length
        )
        if connect_result != 0:
            perror("socket connect")
            raise "Socket connection failed"

    fn _close(self):
        external_call["close", NoneType](self.fd)

    fn __enter__(self) -> Self:
        return self

    # TODO(issue): Terrible error message
    # fn __enter__(owned self) -> Self:
    #     return self

    # TODO(issue): terrible error messages if you have these signatures wrong
    fn __exit__(self):
        self._close()

    fn __exit__(self, _x: Error) -> Bool:
        self._close()
        return False  # re-throw error

    fn send(self, data: StringRef) raises:
        var buffer = data
        while True:
            let result = external_call["send", Int32](
                self.fd, buffer.data, buffer.length, 0
            )
            if result < 0:
                perror("socket send")
                raise "Socket send failed"
            if result >= buffer.length:
                return
            buffer.data += result
            buffer.length -= result.to_int()

    fn recv(self, length: Int = -1) raises -> String:
        alias BUFFER_SIZE = 1024
        var data = String()
        let buffer = DTypePointer[DType.int8].alloc(BUFFER_SIZE)
        while length < 0 or len(data) < length:
            let n = length if length > 0 and length < BUFFER_SIZE else BUFFER_SIZE
            let result = external_call["recv", Int32](self.fd, buffer, n, 0)
            if result < 0:
                perror("socket recv")
                raise "Socket recv failed"
            elif result == 0:
                break
            for i in range(result):
                data._buffer.push_back(buffer[i])
        return data


def main():
    # let addrinfo = getaddrinfo("", "3490")
    # let address = getaddrinfo("www.google.com", "http")
    let address = getaddrinfo("wttr.in", "http")
    print(address)
    with TCPSocket.connect(address) as socket:
        print("sending!")
        socket.send(
            """GET / HTTP/1.0
Host: wttr.in
Accept-Language: en
\n
"""
        )
        print("receiving!")
        print(socket.recv())
