# ===----------------------------------------------------------------------=== #
#
# This file is Modular Inc proprietary.
#
# ===----------------------------------------------------------------------=== #
"""Implements the Hashable trait several hash functions for common data types."""

import math
import random


# This hash secret is XOR-ed with the final hash value for common hash functions.
# Doing so can help prevent DDOS attacks on data structures relying on these
# hash functions. See `hash(bytes, n)` documentation for more details.
# TODO(issue): 'lit.globalvar.ref' op does not refer to a global variable declaration of the right type
# let HASH_SECRET = random.random_ui64(0, math.limit.max_finite[DType.uint64]()).to_int()
# alias HASH_SECRET: Int = 0
alias HASH_SECRET: Int = 0


trait Hashable:
    """A trait for types which specify a function to hash their data.

    This hash function will be used for applications like hash maps, and
    don't need to be cryptographically secure. A good hash function will
    hash similar / common types to different values, and in particular
    the _low order bits_ of the hash, which are used in smaller dictionaries,
    should be sensitive to any changes in the data structure. If your type's
    hash function doesn't meet this criteria it will get poor performance in
    common hash map implementations.

    ```mojo
    @value
    struct Foo(Hashable):
        fn __hash__(self) -> Int:
            return 4  # chosen by fair random dice roll

    let foo = Foo()
    print(hash(foo))
    ```
    """

    fn __hash__(self) -> Int:
        """Return a 64-bit hash of the type's data."""
        ...


fn hash[T: Hashable](hashable: T) -> Int:
    """Hash a Hashable type usings its underlying hash implementation.

    Parameters:
        T: Any Hashable type.
    Args:
        hashable: The input data to hash.
    Returns:
        A 64-bit integer hash based on the underlying implementation.
    """
    return hashable.__hash__()


fn _djbx33a_init[type: DType, size: Int]() -> SIMD[type, size]:
    return SIMD[type, size](5361)


fn _djbx33a_hash_update[
    type: DType, size: Int
](data: SIMD[type, size], next: SIMD[type, size]) -> SIMD[type, size]:
    return data * 33 + next


alias HASH_INIT = _djbx33a_init
alias HASH_UPDATE = _djbx33a_hash_update


fn hash[type: DType, size: Int](data: SIMD[type, size]) -> Int:
    """Hash a SIMD byte vector using direct DJBX33A hash algorithm.

    See `hash(bytes, n)` documentation for more details.

    Parameters:
        type: The SIMD dtype of the input data.
        size: The SIMD width of the input data.
    Args:
        data: The input data to hash.
    Returns:
        A 64-bit integer hash. This hash is _not_ suitable for
        cryptographic purposes, but will have good low-bit
        hash collision statistical properties for common data structures.
    """
    alias int8_size = (type.bitwidth() // DType.int8.bitwidth()) * size
    # Stack allocate bytes for `data` and load it into that memory.
    # Then re-interpret as int8 and pass to the specialized int8 hash function.
    var bytes = StaticIntTuple[(int8_size // 8) + 1]()  # TODO
    let raw_ptr = Pointer.address_of(bytes)
    let ptr = DTypePointer[type](raw_ptr.bitcast[SIMD[type, 1]]())
    ptr.simd_store(data)
    return hash(ptr.bitcast[DType.int8]().simd_load[int8_size]())


fn hash[size: Int](data: SIMD[DType.int8, size]) -> Int:
    """Hash a SIMD byte vector using direct DJBX33A hash algorithm.

    This naively implements DJBX33A, with a hash secret appended at the end.
    The hash secret is computed randomly at compile time, so different executions
    will use different secrets, and thus have different hash outputs. This is
    useful in preventing DDOS attacks against hash functions using a
    non-cryptographic hash function like DJBX33A.

    See `hash(bytes, n)` documentation for more details.

    Parameters:
        size: The SIMD width of the input data.
    Args:
        data: The input data to hash.
    Returns:
        A 64-bit integer hash. This hash is _not_ suitable for
        cryptographic purposes, but will have good low-bit
        hash collision statistical properties for common data structures.
    """
    var hash_data = HASH_INIT[DType.int64, 1]()
    for i in range(size):
        hash_data = HASH_UPDATE(hash_data, data[i].cast[DType.int64]())
    return hash_data.to_int() ^ HASH_SECRET


fn hash(bytes: DTypePointer[DType.int8], n: Int) -> Int:
    """Hash a byte array using a SIMD-modified DJBX33A hash algorithm.

    The DJBX33A algorithm is commonly used for data structures that rely
    on well-distributed hashing for performance. The low order bits of the
    result depend on each byte in the input, meaning that single-byte changes
    will result in a changed hash even when masking out most bits eg. for small
    dictionaries.

    _This hash function is not suitable for cryptographic purposes._ The
    algorithm is easy to reverse and produce deliberate hash collisions.
    We _do_ however initialize a random hash secret which is mixed into
    the final hash output. This can help prevent DDOS attacks on applications
    which make use of this function for dictionary hashing. As a consequence,
    hash values are deterministic within an individual runtime instance ie.
    a value will always hash to the same thing, but in between runs this value
    will change based on the hash secret.

    Standard DJBX33A is:
        - hash = 5361
        - for each byte: hash = 33 * hash + byte
    Instead, for all bytes except trailing bytes that don't align
    to the max SIMD vector width, we
        - interpret those bytes as a SIMD vector
        - apply a vectorized hash: v = 33 * v + bytes_as_simd_value
        - call reduce_add on the final result to get a single hash
        - use this value in fallback for the remaining suffix bytes
        with standard DJBX33A

    Python uses DJBX33A with a hash secret for smaller strings, and
    then the SipHash algorithm for longer strings. The arguments and tradeoffs
    are well documented in PEP 456. We should consider this and deeper
    performance/security tradeoffs as Mojo evolves.

    References:
        - https://en.wikipedia.org/wiki/Non-cryptographic_hash_function
        - https://peps.python.org/pep-0456/
        - https://www.phpinternalsbook.com/php5/hashtables/hash_algorithm.html


    ```mojo
    from random import rand
    let n = 64
    let rand_bytes = DTypePointer[DType.int8].alloc(n)
    rand(rand_bytes, n)
    hash(rand_bytes, n)
    ```

    Args:
        bytes: The byte array to hash.
        n: The length of the byte array.
    Returns:
        A 64-bit integer hash. This hash is _not_ suitable for
        cryptographic purposes, but will have good low-bit
        hash collision statistical properties for common data structures.
    """
    alias type = DType.int64
    alias type_width = type.bitwidth() / DType.int8.bitwidth()
    alias simd_width = simdwidthof[type]()
    alias stride = type_width * simd_width

    # 1. Reinterpret the underlying data as a larger int type
    let simd_data = bytes.bitcast[type]()
    # 2. Copy the tail data (smaller than the SIMD register) into
    #    a final hash state update vector that's stack-allocated.
    # TODO: This is tightly coupled with Int width hand `type`.
    var remaining = StaticIntTuple[simd_width](0)
    let ptr = rebind[DTypePointer[DType.int8]](Pointer.address_of(remaining))
    memcpy(ptr, bytes + (n // simd_width), n - (n // simd_width))
    let last_value = ptr.bitcast[type]().simd_load[simd_width]()

    # 3. Compute DJBX33A, but strided across the SIMD vector width.
    #    This is almost the same as DBJX33A, except:
    #    - The order in which bytes of data update the hash is permuted
    #    - For larger inputs, a small constant number of bytes from the
    #      beginning of the string (3/4 of the first vector load)
    #      have a slightly different power of 33 as a coefficient.
    var hash_data = HASH_INIT[type, simd_width]()
    for i in range(0, n // simd_width, stride):
        let bytes = simd_data.simd_load[simd_width](i)
        hash_data = HASH_UPDATE(hash_data, bytes)
    hash_data = HASH_UPDATE(hash_data, last_value)

    # Now finally, hash the final SIMD vector state. This will also use
    # DJBX33A to make sure that higher-order bits of the vector will
    # mix and impact the low-order bits, and is mathematically necessary
    # for this function to equate to naive DJBX33A.
    return hash(hash_data)
